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
package uk.gov.gchq.palisade.service.palisade;

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

import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.stream.ProducerTopicConfiguration;

/**
 * Loads the Palisade Service.  This  will provide a RESTful service for clients to register a data request.
 * The response will provide a unique URL (at the filtered-resource-service) for resources available for viewing.
 * This is the first in a chain of services that will process the request, with each taking on a singular task
 * accumulating at the end with the data that is permitted, filtered, or redacted for each specific request.
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class})
public class PalisadeApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeApplication.class);
    private final RunnableGraph<Sink<TokenRequestPair, NotUsed>> runner;
    private final PalisadeService palisadeService;
    private final Materializer materialiser;

    /**
     * Autowire Akka objects in constructor for application ready event
     *
     * @param runner          the runner
     * @param materialiser    the Akka {@link Materializer} configured to be used
     * @param palisadeService the palisade service
     */
    public PalisadeApplication(
            final RunnableGraph<Sink<TokenRequestPair, NotUsed>> runner,
            final Materializer materialiser,
            final PalisadeService palisadeService) {
        this.runner = runner;
        this.materialiser = materialiser;
        this.palisadeService = palisadeService;
    }

    /**
     * Application entrypoint, creates and runs a spring application, passing in the given command-line args
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        LOGGER.debug("PalisadeApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(PalisadeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    /**
     * Runs all available Akka {@link RunnableGraph}s until completion.
     * The 'main' threads of the application during runtime are the completable futures spawned here.
     * It will run after the application is 'ready' and fully loaded.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        palisadeService.registerRequestSink(runner.run(materialiser));
    }
}

