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
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.gchq.palisade.service.data.web.AkkaHttpServer;

import javax.annotation.PreDestroy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Starter for the Data Service. Will start the service and initialise all the components needed to run the service.
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class})
public class DataApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApplication.class);

    private final AuditMessageService auditMessageService;
    private final DataReader dataReader;
    private final StdSerialiserConfiguration serialiserConfiguration;

    private final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner;
    private final Set<ActorSystem> actorSystems;
    private final AkkaHttpServer server;
    private final ActorSystem system;
    private final Materializer materialiser;
    private final Executor executor;
    private final Set<CompletableFuture<?>> runnerThreads = new HashSet<>();

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param auditMessageService     instance of the auditMessageService to connect with the sink from the runner
     * @param dataReader              reader to 'prepopulate' with serialisers
     * @param serialiserConfiguration serialisers to add to the reader
     * @param runner                  collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param actorSystems            collection of all Akka {@link ActorSystem}s discovered for the application
     * @param server                  the http server to start (in replacement of spring-boot-starter-web)
     * @param system                  the default akka actor system
     * @param executor                an executor for any {@link CompletableFuture}s (preferably the application task executor)
     */
    public DataApplication(
            final AuditMessageService auditMessageService,
            final DataReader dataReader,
            final StdSerialiserConfiguration serialiserConfiguration,
            final RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner,
            final Collection<ActorSystem> actorSystems,
            final AkkaHttpServer server,
            final ActorSystem system,
            final @Qualifier("applicationTaskExecutor") Executor executor) {
        this.auditMessageService = auditMessageService;
        this.dataReader = dataReader;
        this.serialiserConfiguration = serialiserConfiguration;

        this.runner = runner;
        this.actorSystems = new HashSet<>(actorSystems);
        this.server = server;
        this.system = system;
        this.executor = executor;
        this.materialiser = Materializer.createMaterializer(system);
    }

    /**
     * Application entry point.
     *
     * @param args from the command line
     */
    public static void main(final String[] args) {
        LOGGER.debug("DataApplication started with: {}", (Object) args);

        new SpringApplicationBuilder(DataApplication.class)
                .web(WebApplicationType.NONE)
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
        runnerThreads.add(CompletableFuture.supplyAsync(() -> runner.run(materialiser), executor));
        auditMessageService.registerRequestSink(runner.run(materialiser));
        LOGGER.info("Started {} runner threads", runnerThreads.size());

        // Add serialiser to the Data Service
        LOGGER.info("Pre-populating using serialiser config: {}", serialiserConfiguration);
        serialiserConfiguration.getSerialisers().stream()
                .map(StdSerialiserPrepopulationFactory::build)
                .forEach((Entry<DataFlavour, Serialiser<Object>> entry) -> {
                    dataReader.addSerialiser(entry.getKey(), entry.getValue());
                    LOGGER.info("Added serialiser to data-reader for flavour {}", entry.getKey());
                    LOGGER.debug("Added {} -> {}", entry.getKey(), entry.getValue());
                });

        server.serveForever(system);

        runnerThreads.forEach(CompletableFuture::join);
    }

    /**
     * Cancels any futures that are running and then terminates the Akka Actor so the service can be terminated safely
     */
    @PreDestroy
    public void onExit() {
        LOGGER.info("Terminating (persistence controller) actor systems");
        actorSystems.forEach(ActorSystem::terminate);
        LOGGER.info("Cancelling running futures");
        runnerThreads.forEach(thread -> thread.cancel(true));
        LOGGER.info("Shutting down HTTP server");
        server.terminate();
        LOGGER.info("Terminating (root) actor system");
        system.terminate();
    }
}
