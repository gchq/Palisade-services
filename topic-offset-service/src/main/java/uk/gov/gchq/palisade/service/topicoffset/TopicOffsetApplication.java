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
package uk.gov.gchq.palisade.service.topicoffset;

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
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.service.topicoffset.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.topicoffset.stream.ProducerTopicConfiguration;

import javax.annotation.PreDestroy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Topic Offset Service is a performance optimisation for the stream message process.  The service will look for the
 * indication that this message is the first of a set of response messages for a specific request. It will
 * be watching for a Kafka header with the message {Stream-Marker=Start, Token=xxxx-xxxx-xxxx}. It will take this
 * information along with the commit offset of this stream and this will be written to the downstream queue.  This can
 * then be used to optimise the start up client connections by the filtered-resource-service.
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class TopicOffsetApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicOffsetApplication.class);

    private final Set<RunnableGraph<?>> runners;
    private final Materializer materialiser;
    private final Executor executor;
    private final Set<CompletableFuture<?>> runnerThreads = new HashSet<>();

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runners      collection of all Akka {@link RunnableGraph}s discovered for the application
     * @param materialiser the Akka {@link Materializer} configured to be used
     * @param executor     an executor for any {@link CompletableFuture}s (preferably the application task executor)
     */
    public TopicOffsetApplication(
            final Collection<RunnableGraph<?>> runners,
            final Materializer materialiser,
            @Qualifier("applicationTaskExecutor") final Executor executor) {
        this.runners = new HashSet<>(runners);
        this.materialiser = materialiser;
        this.executor = executor;
    }

    /**
     * Starts the Topic Offset Service
     *
     * @param args required input for the main method
     */
    public static void main(final String[] args) {
        LOGGER.debug("TopicOffsetApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(TopicOffsetApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * Runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        runnerThreads.addAll(runners.stream()
                .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(materialiser), executor))
                .collect(Collectors.toSet()));
        LOGGER.info("Started {} runner threads", runnerThreads.size());
        runnerThreads.forEach(CompletableFuture::join);
    }

    /**
     * Cancels any futures that are running and then terminates the Akka Actor so the service can be terminated safely
     */
    @PreDestroy
    public void onExit() {
        LOGGER.info("Cancelling running futures");
        runnerThreads.forEach(thread -> thread.cancel(true));
        LOGGER.info("Terminating actor system");
        materialiser.system().terminate();
    }
}
