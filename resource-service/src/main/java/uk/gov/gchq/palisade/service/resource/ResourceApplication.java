/*
 * Copyright 2020 Crown Copyright
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

import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.service.resource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Application entrypoint and main process runner
 */
@EnableDiscoveryClient
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
     * @param runners       collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materializer  the Akka {@link Materializer} configured to be used
     * @param persistence     a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param executor      an executor for any {@link CompletableFuture}s (preferably the application task executor)
     * @param resourceBuilder a {@link Supplier} of resources as built by a {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory}, but with a connection detail attached
     */
    public ResourceApplication(final Collection<RunnableGraph<?>> runners,
                               final Materializer materializer,
                               final PersistenceLayer persistence,
                               @Qualifier("configuredResourceBuilder") final Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder,
                               @Qualifier("applicationTaskExecutor") final Executor executor) {
        this.runners = new HashSet<>(runners);
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
     * Adds resource(s) from a configuration file to the persistence of the {@link uk.gov.gchq.palisade.service.ResourceService}
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        // Add resources to persistence
        LOGGER.info("Prepopulating using resource builder: {}", resourceBuilder);
        resourceBuilder.get()
                .forEach(entry -> {
                    Resource rootResource = entry.getKey();
                    LeafResource leafResource = entry.getValue();
                    LOGGER.info("Persistence add for {} -> {}", rootResource.getId(), leafResource.getId());
                    FunctionalIterator<LeafResource> resourceIterator = FunctionalIterator.fromIterator(Collections.singletonList(leafResource).iterator());
                    resourceIterator = persistence.withPersistenceById(rootResource.getId(), resourceIterator);
                    resourceIterator = persistence.withPersistenceByType(leafResource.getType(), resourceIterator);
                    resourceIterator = persistence.withPersistenceBySerialisedFormat(leafResource.getSerialisedFormat(), resourceIterator);
                    while (resourceIterator.hasNext()) {
                        LeafResource resource = resourceIterator.next();
                        LOGGER.debug("Resource {} persisted", resource.getId());
                    }
                });
    }

    /**
     * Runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serverForever() {
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());

        runnerThreads.forEach(CompletableFuture::join);
    }
}
