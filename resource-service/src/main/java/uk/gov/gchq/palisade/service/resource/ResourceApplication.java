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

package uk.gov.gchq.palisade.service.resource;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.reader.common.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.reader.common.ResourceService;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Application entrypoint and main process runner
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class ResourceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceApplication.class);

    private final Set<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;
    private final PersistenceLayer persistence;
    private final Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder;

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runners         collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materializer    the Akka {@link Materializer} configured to be used
     * @param persistence     a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param executor        an executor for any {@link CompletableFuture}s (preferably the application task executor)
     * @param resourceBuilder a {@link Supplier} of resources as built by a {@link ResourcePrepopulationFactory},
     *                        but with a connection detail attached
     */
    public ResourceApplication(final Set<RunnableGraph<?>> runners,
                               final Materializer materializer,
                               final PersistenceLayer persistence,
                               @Qualifier("configuredResourceBuilder") final Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder,
                               @Qualifier("threadPoolTaskExecutor") final Executor executor) {
        this.runners = Collections.unmodifiableSet(runners);
        this.materializer = materializer;
        this.persistence = persistence;
        this.executor = executor;
        this.resourceBuilder = resourceBuilder;
    }

    /**
     * Application entrypoint, creates and runs a spring application, passing in the given command-line args
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        LOGGER.debug("ResourceApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(ResourceApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * This method perform 2 actions on the {@link ApplicationReadyEvent}
     * <ol>
     *     <li>Adds resource(s) from a configuration file to the persistence of the {@link ResourceService}</li>
     *     <li>Runs all available Akka {@link RunnableGraph}s until completion.</li>
     * </ol>
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serverForever() {
        // Add resources to persistence
        LOGGER.info("Prepopulating using resource builder: {}", resourceBuilder);

        resourceBuilder.get()
                .forEach((Entry<Resource, LeafResource> entry) -> {
                    Resource rootResource = entry.getKey();
                    LeafResource leafResource = entry.getValue();
                    Sink<LeafResource, CompletionStage<Done>> loggingSink = Sink.foreach(
                            persistedResource -> LOGGER.info("Persistence add for {} -> {}", rootResource.getId(), persistedResource.getId()));
                    Source.single(leafResource)
                            .via(persistence.withPersistenceById(rootResource.getId()))
                            .via(persistence.withPersistenceByType(leafResource.getType()))
                            .via(persistence.withPersistenceBySerialisedFormat(leafResource.getSerialisedFormat()))
                            .runWith(loggingSink, materializer);
                });

        // Then start up kafka
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());

        runnerThreads.forEach(CompletableFuture::join);
    }
}
