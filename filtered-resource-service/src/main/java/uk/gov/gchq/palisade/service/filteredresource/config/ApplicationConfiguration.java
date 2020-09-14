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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.filteredresource.repository.JpaTopicOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TopicOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TopicOffsetRepository;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorReporterDaemon;
import uk.gov.gchq.palisade.service.filteredresource.service.FilteredResourceService;
import uk.gov.gchq.palisade.service.filteredresource.service.SimpleTopicOffsetDaemon;

import java.util.concurrent.CompletableFuture;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    JpaTopicOffsetPersistenceLayer jpaTopicOffsetPersistenceLayer(final TopicOffsetRepository repository) {
        return new JpaTopicOffsetPersistenceLayer(repository);
    }

    @Bean
    ErrorReporterDaemon loggingErrorReporterDaemon() {
        return (String token, Throwable exception) -> LOGGER.error("An error was reported for token {}:", token, exception);
    }

    @Bean
    SimpleTopicOffsetDaemon simpleTopicOffsetDaemon(final TopicOffsetPersistenceLayer persistenceLayer) {
        return new SimpleTopicOffsetDaemon(persistenceLayer);
    }

    @Bean
    FilteredResourceService loggingFilteredResourceService() {
        return (String token) -> CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Spawned a logging process for token {}", token);
            return null;
        });
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
