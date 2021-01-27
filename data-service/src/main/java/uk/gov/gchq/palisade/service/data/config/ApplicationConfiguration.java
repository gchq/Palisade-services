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
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    // Replace this with a proper error handling mechanism (kafka queues etc.)
    @Bean
    ErrorHandlingService loggingErrorHandler() {
        LOGGER.warn("Using a Logging-only error handler, this should be replaced by a proper implementation!");
        return (String token, AuditErrorMessage message) -> LOGGER.error("Token {} and resourceId {} threw exception", token, message.getResourceId(), message.getError());
    }

    // Replace this with a proper audit mechanism (kafka queues etc.)
    @Bean
    AuditService loggingAuditService() {
        LOGGER.warn("Using a Logging-only auditor, this should be replaced by a proper implementation!");
        return (String token, AuditSuccessMessage message) -> LOGGER.warn("Token {} and resourceId {} read leafResourceId {}", token, message.getResourceId(), message.getLeafResourceId());
    }

    /**
     * Bean for the {@link JpaPersistenceLayer}.
     * Connect the Redis or Caffeine backed repository to the persistence layer, providing an executor for any async requests
     *
     * @param requestsRepository an instance of the requests repository, backed by either caffeine or redis (depending on profile)
     * @param executor           an async executor, preferably a {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
     * @return a {@link JpaPersistenceLayer} wrapping the repository instance, providing async methods for getting data from persistence
     */
    @Bean
    JpaPersistenceLayer jpaPersistenceLayer(final AuthorisedRequestsRepository requestsRepository,
                                            final @Qualifier("threadPoolTaskExecutor") Executor executor) {
        return new JpaPersistenceLayer(requestsRepository, executor);
    }

    /**
     * Bean for a {@link SimpleDataService}, connecting a {@link DataReader} and {@link PersistenceLayer}.
     * These are likely the {@code HadoopDataReader} and the {@link JpaPersistenceLayer}.
     *
     * @param persistenceLayer the persistence layer for reading authorised requests
     * @param dataReader       the data reader to use for reading resource data from storage
     * @return a new {@link SimpleDataService}
     */
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

    /**
     * Default JSON to Java seraialiser/deserialiser
     *
     * @return a new {@link ObjectMapper} with some additional configuration
     */
    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
