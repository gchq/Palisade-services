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
package uk.gov.gchq.palisade.service.filteredresource;

import akka.actor.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;

/**
 * SpringBoot application entry-point method for the {@link FilteredResourceApplication} executable
 */
@SpringBootApplication
@EnableConfigurationProperties({ProducerTopicConfiguration.class, ConsumerTopicConfiguration.class})
public class FilteredResourceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilteredResourceApplication.class);

    private final ActorSystem system;
    private final AkkaHttpServer server;

    /**
     * Autowire Akka objects in constructor for application ready event
     */
    public FilteredResourceApplication(final ActorSystem system, final AkkaHttpServer server) {
        this.system = system;
        this.server = server;
    }

    /**
     * Application entrypoint, creates and runs a spring application, passing in the given command-line args
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        LOGGER.debug("FilteredResourceApplication started with: {}", (Object) args);
        new SpringApplicationBuilder(FilteredResourceApplication.class).web(WebApplicationType.NONE)
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void serveForever() {
        this.server.serveForever(this.system);
    }
}
