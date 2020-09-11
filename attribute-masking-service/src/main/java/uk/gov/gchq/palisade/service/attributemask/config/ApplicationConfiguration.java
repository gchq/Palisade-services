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

package uk.gov.gchq.palisade.service.attributemask.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.attributemask.service.SimpleAttributeMaskingService;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    JpaPersistenceLayer persistenceLayer(final AuthorisedRequestsRepository authorisedRequestsRepository) {
        return new JpaPersistenceLayer(authorisedRequestsRepository);
    }

    @Bean
    SimpleAttributeMaskingService simpleAttributeMaskingService(final JpaPersistenceLayer persistenceLayer) {
        return new SimpleAttributeMaskingService(persistenceLayer);
    }

    // TODO: Replace this with a proper error handling mechanism (ie. kafka error queue)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (token, request, error) -> {
            LOGGER.error("Token {} and request {} threw exception {}", token, request, error);
        };
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
