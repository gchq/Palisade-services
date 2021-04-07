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

import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;

/**
 * Starter for the Data Service.  Will start the service and initalise all of the components needed to run the service.
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class})
public class DataApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApplication.class);

    private final DataReader dataReader;
    private final AuditMessageService auditMessageService;

    private final StdSerialiserConfiguration serialiserConfiguration;
    private final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner;
    private final Materializer materializer;

    /**
     * Autowires Akka objects and the seraliser for the data reader.  These are initalised after the application has
     * been started.
     *
     * @param dataReader              a reader for retrieving the request resources.
     * @param auditMessageService     service for sending audit success and error messages
     * @param serialiserConfiguration a configuration and initialising the {@link DataReader}
     * @param runner                  runnable graphs for sending messages on the Kafka stream
     * @param materializer            the Akka {@link Materializer} configured to be used
     */
    public DataApplication(
            final DataReader dataReader,
            final AuditMessageService auditMessageService,
            final StdSerialiserConfiguration serialiserConfiguration,
            final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner,
            final Materializer materializer) {

        this.dataReader = dataReader;
        this.auditMessageService = auditMessageService;
        this.serialiserConfiguration = serialiserConfiguration;
        this.runner = runner;
        this.materializer = materializer;
    }

    /**
     * Application entry point
     *
     * @param args from the command line
     */
    public static void main(final String[] args) {
        LOGGER.debug("DataApplication started with: {}", (Object) args);

        new SpringApplicationBuilder(DataApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * Performs the tasks that need to be done after Spring initialisation.  This includes the configuration of the
     * serialiser and the starting of the Kafka consumer needed for sending audit messages to the Audit Service.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {

        //start the Kafka consumer for sending success and error messages to audit-service
        auditMessageService.registerRequestSink(runner.run(materializer));

        // Add serialiser to the data-service
        LOGGER.debug("Pre-populating using serialiser config: {}", serialiserConfiguration.getClass());
        serialiserConfiguration.getSerialisers().stream()
                .map(StdSerialiserPrepopulationFactory::build)
                .forEach(entry -> dataReader.addSerialiser(entry.getKey(), entry.getValue()));
    }
}
