/*
 * Copyright 2018-2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.service.data;

import akka.NotUsed;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.reader.DataFlavour;
import uk.gov.gchq.palisade.service.data.reader.DataReader;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;

import javax.annotation.PreDestroy;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

/**
 * Starter for the Data Service. Will start the service and initialise all the components needed to run the service.
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class})
public class DataApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApplication.class);

    private final DataReader dataReader;
    private final AuditMessageService auditMessageService;

    private final StdSerialiserConfiguration serialiserConfiguration;
    private final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner;
    private final Materializer materialiser;
    private CompletableFuture<?> runnerThread = new CompletableFuture<>();

    /**
     * Constructor for {@code DataApplication}.
     *
     * @param dataReader              a reader for retrieving the request resources.
     * @param auditMessageService     service for sending audit success and error messages to the Audit Service
     * @param serialiserConfiguration a configuration and initialising the {@link DataReader}
     * @param runner                  runnable graphs for sending messages on the Kafka stream
     * @param materialiser            the Akka {@link Materializer} responsible for turning a stream blueprint into a
     *                                running stream.
     */
    public DataApplication(
            final DataReader dataReader,
            final AuditMessageService auditMessageService,
            final StdSerialiserConfiguration serialiserConfiguration,
            final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner,
            final Materializer materialiser) {

        this.dataReader = dataReader;
        this.auditMessageService = auditMessageService;
        this.serialiserConfiguration = serialiserConfiguration;
        this.runner = runner;
        this.materialiser = materialiser;
    }

    /**
     * Application entry point.
     *
     * @param args from the command line
     */
    public static void main(final String[] args) {
        LOGGER.debug("DataApplication started with: {}", (Object) args);

        new SpringApplicationBuilder(DataApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * Performs the tasks that need to be done after Spring initialisation and before running the service. This
     * includes the configuration of the serialiser, and the starting of the Kafka sinks used for sending audit
     * messages to the Audit Service.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {

        //start the Kafka sink for sending success and error messages to Audit Service
        runnerThread = CompletableFuture.supplyAsync(() -> runner.run(materialiser));
        auditMessageService.registerRequestSink(runner.run(materialiser));

        // Add serialiser to the Data Service
        LOGGER.info("Pre-populating using serialiser config: {}", serialiserConfiguration);
        serialiserConfiguration.getSerialisers().stream()
                .map(StdSerialiserPrepopulationFactory::build)
                .forEach((Entry<DataFlavour, Serialiser<Object>> entry) -> {
                    dataReader.addSerialiser(entry.getKey(), entry.getValue());
                    LOGGER.info("Added serialiser to data-reader for flavour {}", entry.getKey());
                    LOGGER.debug("Added {} -> {}", entry.getKey(), entry.getValue());
                });
    }

    /**
     * Cancels any futures that are running and then terminates the Akka Actor, so the service can be terminated safely
     */
    @PreDestroy
    public void onExit() {
        LOGGER.info("Cancelling running futures");
        runnerThread.cancel(true);
        LOGGER.info("Terminating actor system");
        materialiser.system().terminate();
    }
}
