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

package uk.gov.gchq.palisade.service.user;

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

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.common.service.UserConfiguration;
import uk.gov.gchq.palisade.service.user.common.service.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.service.UserServiceCachingProxy;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * SpringBoot application entry-point method for the {@link UserApplication} executable
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class UserApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserApplication.class);

    private final Set<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;
    private final UserServiceCachingProxy service;
    private final UserConfiguration userConfig;

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runners       collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materializer  the Akka {@link Materializer} configured to be used
     * @param service       the specific {@link UserService} implementation
     * @param configuration the {@link UserConfiguration} required for loading {@link User}s into the service
     * @param executor      an executor for any {@link CompletableFuture}s (preferably the application task executor)
     */
    public UserApplication(
            final Collection<RunnableGraph<?>> runners,
            final Materializer materializer,
            final UserServiceCachingProxy service,
            @Qualifier("userConfiguration") final UserConfiguration configuration,
            @Qualifier("threadPoolTaskExecutor") final Executor executor) {
        this.service = service;
        this.userConfig = configuration;
        this.runners = new HashSet<>(runners);
        this.materializer = materializer;
        this.executor = executor;
    }

    /**
     * Application entrypoint, creates and runs a spring application, passing in the given command-line args
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        LOGGER.debug("UserApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(UserApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * First pre-populates the cache using a configuration yaml file
     * Then runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        // First pre-populate the cache
        LOGGER.info("Pre-populating using user config: {}", userConfig.getClass());
        // Add example users to the user-service cache
        userConfig.getUsers().stream()
                .map(UserPrepopulationFactory::build)
                .peek(user -> LOGGER.debug(user.toString()))
                .forEach(service::addUser);

        // Then start up kafka
        Set<CompletableFuture<?>> runnerThreads = runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materializer), executor))
                .collect(Collectors.toSet());
        LOGGER.info("Started {} runner threads", runnerThreads.size());
        runnerThreads.forEach(CompletableFuture::join);
    }
}
