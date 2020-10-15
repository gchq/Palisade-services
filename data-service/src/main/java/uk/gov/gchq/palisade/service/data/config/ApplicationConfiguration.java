/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.HadoopDataReader;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.common.SerialisedDataReader;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    // Replace this with a proper error handling mechanism (kafka queues etc.)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (String token, AuditErrorMessage message) -> LOGGER.error("Token {} and resourceId {} threw exception {}", token, message.getResourceId(), message.getError());
    }

    // Replace this with a proper audit mechanism (kafka queues etc.)
    @Bean
    AuditService loggingAuditService() {
        LOGGER.warn("Using a Logging-only auditor, this should be replaced by a proper implementation!");
        return (String token, AuditSuccessMessage message) -> LOGGER.warn("Token {} and resourceId {} read leafResourceId {}", token, message.getResourceId(), message.getLeafResourceId());
    }

    @Bean
    JpaPersistenceLayer jpaPersistenceLayer(final AuthorisedRequestsRepository requestsRepository,
                                            final @Qualifier("threadPoolTaskExecutor") Executor executor) {
        return new JpaPersistenceLayer(requestsRepository, executor);
    }

    @Bean
    SimpleDataService simpleDataService(final PersistenceLayer persistenceLayer,
                                        final DataReader dataReader) {
        return new SimpleDataService(persistenceLayer, dataReader);
    }

    /**
     * Bean implementation for {@link HadoopDataReader} which extends {@link SerialisedDataReader} and is used for setting hadoopConfigurations and reading raw data.
     *
     * @return a new instance of {@link HadoopDataReader}
     * @throws IOException ioException
     */
    @Bean
    DataReader hadoopDataReader() throws IOException {
        return new HadoopDataReader();
    }

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
