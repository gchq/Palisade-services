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
package uk.gov.gchq.palisade.service.attributemask;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class AttributeMaskingApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMaskingApplication.class);

    private final Collection<RunnableGraph<?>> runners;
    private final Materializer materializer;
    private final Executor executor;
    private final ActorSystem system;

    public AttributeMaskingApplication(
            final Collection<RunnableGraph<?>> runners,
            final ActorSystem system,
            final Materializer materializer,
            @Qualifier("applicationTaskExecutor") final Executor executor) {
        this.runners = runners;
        this.system = system;
        this.materializer = materializer;
        this.executor = executor;
    }

    public static void main(final String[] args) {
        LOGGER.debug("AttributeMaskingApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(AttributeMaskingApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
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

        Config conf = system.settings().config().getConfig("akka");
        conf.entrySet().forEach(val -> LOGGER.debug("{} = {}", val.getKey(), val.getValue()));
        runnerThreads.forEach(CompletableFuture::join);
    }

}
