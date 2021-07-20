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

import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.DataService;
import uk.gov.gchq.palisade.service.data.service.ReadChunkedDataService;
import uk.gov.gchq.palisade.service.data.service.authorisation.AuditableAthorisationService;
import uk.gov.gchq.palisade.service.data.service.authorisation.AuthorisationService;
import uk.gov.gchq.palisade.service.data.service.authorisation.SimpleAuthorisationService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;
import uk.gov.gchq.palisade.service.data.service.reader.SimpleDataReader;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph.
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    private static final int CORE_POOL_SIZE = 6;

    @Bean
    @ConfigurationProperties(prefix = "data")
    SerialiserConfiguration serialiserConfiguration() {
        return new SerialiserConfiguration();
    }

    @Bean
    @ConditionalOnProperty(prefix = "data", name = "implementation", havingValue = "simple", matchIfMissing = true)
    DataReader simpleDataReader() {
        return new SimpleDataReader();
    }

    /**
     * Bean for the {@link JpaPersistenceLayer}.
     * Connect the Redis or Caffeine backed repository to the persistence layer, providing an executor for any async requests.
     *
     * @param requestsRepository an instance of the requests repository, backed by either caffeine or redis (depending on profile)
     * @param executor           an async executor, preferably a {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
     * @return a {@link JpaPersistenceLayer} wrapping the repository instance, providing async methods for getting data from persistence
     */
    @Bean
    JpaPersistenceLayer jpaPersistenceLayer(final AuthorisedRequestsRepository requestsRepository,
                                            final @Qualifier("applicationTaskExecutor") Executor executor) {
        return new JpaPersistenceLayer(requestsRepository, executor);
    }

    /**
     * Bean for a {@link SimpleAuthorisationService}, connecting a {@link DataReader} and {@link PersistenceLayer}.
     * These are likely the {@code HadoopDataReader} and the {@link JpaPersistenceLayer}.
     *
     * @param persistenceLayer the persistence layer for reading authorised requests
     * @return a new {@link SimpleAuthorisationService}
     */
    @Bean
    AuthorisationService simpleAuthorisationService(final PersistenceLayer persistenceLayer) {
        return new SimpleAuthorisationService(persistenceLayer);
    }

    @Bean
    AuditableAthorisationService auditableAuthorisationService(final AuthorisationService authorisationService) {
        return new AuditableAthorisationService(authorisationService);
    }

    @Bean
    AuditMessageService auditService(final Materializer materializer) {
        return new AuditMessageService(materializer);
    }

    @Bean
    DataService readChunkedDataService(final Collection<DataReader> readers, final SerialiserConfiguration serialiserConfiguration,
                                       final AuditableAthorisationService dataService, final AuditMessageService auditService) {
        return new ReadChunkedDataService(readers, serialiserConfiguration.getSerialiserClassMap(), dataService, auditService);
    }

    /**
     * Default JSON to Java seraialiser/deserialiser.
     *
     * @return a new {@link ObjectMapper} with some additional configuration
     */
    @Bean
    @Primary
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("applicationTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(CORE_POOL_SIZE);
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }
}
