/*
 * Copyright 2019 Crown Copyright
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

import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Starter for the data-service.  Will start the service and initalise all of the components needed to run the service.
 */
@SpringBootApplication
public class DataApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApplication.class);

    private final DataReader dataReader;
    private final StdSerialiserConfiguration serialiserConfiguration;
    private final Set<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;

    /**
     * @param dataReader a reader for retrieving the request resources.
     * @param serialiserConfiguration a configuration and initialising the {@link DataReader}
     * @param runners runnable graphs for sending messages on the Kafka stream
     * @param materializer the Akka {@link Materializer} configured to be used
     * @param executor used for asynchronous processing of {@link CompletableFuture}s
     */
    public DataApplication(
            final DataReader dataReader,
            final StdSerialiserConfiguration serialiserConfiguration,
            final Set<RunnableGraph<?>> runners,
            final Materializer materializer,
            final Executor executor) {

        this.dataReader = dataReader;
        this.serialiserConfiguration = serialiserConfiguration;
        this.runners = runners;
        this.materializer = materializer;
        this.executor = executor;
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
     * serialiser and the starting of the Kafka consumer used for sending audit messages to the audit-service.
     */
    @EventListener(ApplicationReadyEvent.class)

    public void initPostConstruct() {

        // Add serialiser to the data-service
        LOGGER.debug("Prepopulating using serialiser config: {}", serialiserConfiguration.getClass());
        serialiserConfiguration.getSerialisers().stream()
                .map(StdSerialiserPrepopulationFactory::build)
                .peek(entry -> LOGGER.debug(entry.toString()))
                .forEach(entry -> dataReader.addSerialiser(entry.getKey(), entry.getValue()));

        //start the Kafka consumer for sending success and error messages to audit-service
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());
        runnerThreads.forEach(CompletableFuture::join);
    }
}
