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
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.policy.common.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Application entrypoint and main process runner
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class PolicyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyApplication.class);

    private final Set<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;
    private final PolicyServiceCachingProxy service;
    private final PolicyConfiguration policyConfig;

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runners      collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materializer the Akka {@link Materializer} configured to be used
     * @param service      specifically policyServiceCachingProxy used for pre-population
     * @param policyConfig resourceConfig used to create the policy object used in pre-population
     * @param executor     an executor for any {@link CompletableFuture}s (preferably the application task executor)
     */
    public PolicyApplication(
            final Collection<RunnableGraph<?>> runners,
            final Materializer materializer,
            final PolicyServiceCachingProxy service,
            final PolicyConfiguration policyConfig,
            @Qualifier("threadPoolTaskExecutor") final Executor executor) {
        this.runners = new HashSet<>(runners);
        this.materializer = materializer;
        this.service = service;
        this.policyConfig = policyConfig;
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
     * Runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        //Pre-populate the cache
        LOGGER.debug("Pre-populating using policy config: {}", policyConfig.getClass());
        policyConfig.getPolicies()
                .forEach((PolicyPrepopulationFactory factory) -> {
                    //Build Resource Rules
                    Entry<String, Rules<LeafResource>> resourceMap = factory.buildResourceRules();
                    service.setResourceRules(resourceMap.getKey(), resourceMap.getValue());

                    //Build Record Rules
                    Entry<String, Rules<Serializable>> recordMap = factory.buildRecordRules();
                    service.setRecordRules(recordMap.getKey(), recordMap.getValue());
                });

        //Then start up kafka
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());
        runnerThreads.forEach(CompletableFuture::join);
    }
}
