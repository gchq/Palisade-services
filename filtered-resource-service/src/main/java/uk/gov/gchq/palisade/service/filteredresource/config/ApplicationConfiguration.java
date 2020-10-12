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

package uk.gov.gchq.palisade.service.filteredresource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.repository.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetRepository;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.filteredresource.service.FilteredResourceService;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.WebsocketEventService;

import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    TokenOffsetPersistenceLayer jpaTokenOffsetPersistenceLayer(
            final TokenOffsetRepository repository,
            final @Qualifier("threadPoolTaskExecutor") Executor executor) {
        return new JpaTokenOffsetPersistenceLayer(repository, executor);
    }

    // TODO: Replace this with a proper error reporting service (akka actors etc.)
    @Bean
    ErrorEventService loggingErrorReporterService() {
        return (String token, Throwable exception) -> LOGGER.error("An error was reported for token {}:", token, exception);
    }

    @Bean
    OffsetEventService topicOffsetService(final TokenOffsetPersistenceLayer persistenceLayer) {
        return new OffsetEventService(persistenceLayer);
    }

    // TODO: Replace this with a proper filtered resource service (websockets etc.)
    @Bean
    FilteredResourceService loggingFilteredResourceService(final TokenOffsetPersistenceLayer persistenceLayer) {
        return (String token) -> {
            LOGGER.info("Ignoring token {} since there's no real websocketEventService", token);
            return new WebsocketEventService(persistenceLayer);
        };
    }

    // TODO: Replace this with a proper error handling mechanism (kafka queues etc.)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (String token, FilteredResourceRequest request, Throwable error) -> LOGGER.error("Token {} and request {} threw exception {}", token, request, error.getMessage());
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
