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

package uk.gov.gchq.palisade.service.policy;

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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.UserConfiguration;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Application entrypoint and main process runner
 */
@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class PolicyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyApplication.class);

    private final Set<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;
    private final PolicyServiceCachingProxy service;
    private final PolicyConfiguration policyConfig;
    private final UserConfiguration userConfig;
    private final ResourceConfiguration resourceConfig;

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runners        collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materializer   the Akka {@link Materializer} configured to be used
     * @param service        specifically policyServiceCachingProxy used for pre-population
     * @param policyConfig   resourceConfig used to create the policy object used in pre-population
     * @param userConfig     resourceConfig used to create the user object used in pre-population
     * @param resourceConfig resourceConfig used to create the resource object used in pre-population
     * @param executor       an executor for any {@link CompletableFuture}s (preferably the application task executor)
     */
    public PolicyApplication(
            final Collection<RunnableGraph<?>> runners,
            final Materializer materializer,
            final PolicyServiceCachingProxy service,
            final PolicyConfiguration policyConfig,
            final UserConfiguration userConfig,
            final ResourceConfiguration resourceConfig,
            @Qualifier("threadPoolTaskExecutor") final Executor executor) {
        this.runners = new HashSet<>(runners);
        this.materializer = materializer;
        this.service = service;
        this.policyConfig = policyConfig;
        this.userConfig = userConfig;
        this.resourceConfig = resourceConfig;
        this.executor = executor;
    }

    /**
     * Application entrypoint, creates and runs a spring application, passing in the given command-line args
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        LOGGER.debug("PolicyApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(PolicyApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * Init post construct.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Profile({"pre-population, example"})
    // Pre-population profile used in the KafkaContractTests for pre-populating the cache. Example profile used for pre-populating the cache for the example
    public void initPostConstruct() {
        // Add example Policies to the policy-service cache
        LOGGER.debug("Pre-populating using policy config: {}", policyConfig.getClass());
        LOGGER.debug("Pre-populating using user config: {}", userConfig.getClass());
        LOGGER.debug("Pre-populating using resource config: {}", resourceConfig.getClass());
        policyConfig.getPolicies().stream()
                .peek(e -> LOGGER.info("pre-pop stream {}", e))
                .map(prepopulation -> prepopulation.build(userConfig.getUsers(), resourceConfig.getResources()))
                .peek(entry -> LOGGER.debug("pre-pop entry {}", entry))
                .forEach(entry -> service.setResourcePolicy(entry.getKey(), entry.getValue()));
    }

    /**
     * Runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());
        runnerThreads.forEach(CompletableFuture::join);
    }


}
