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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.reader.DataFlavour;
import uk.gov.gchq.palisade.service.data.reader.DataReader;
import uk.gov.gchq.palisade.service.data.reader.SimpleDataReader;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.service.DataService;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph.
 */
@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    private static final int CORE_POOL_SIZE = 6;

    /**
     * A {@link StdSerialiserConfiguration} object that uses Spring to configure a list of serialisers from a yaml file.
     * A container for a number of {@link StdSerialiserPrepopulationFactory} builders used for creating {@link uk.gov.gchq.palisade.data.serialise.Serialiser}s.
     * These serialisers will be used for prepopulating the {@link uk.gov.gchq.palisade.service.data.service.DataService}.
     *
     * @return a {@link StdSerialiserConfiguration} containing a list of {@link StdSerialiserPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdSerialiserConfiguration serialiserConfiguration() {
        return new StdSerialiserConfiguration();
    }

    /**
     * Implementation of a {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file.
     * A factory for {@link Serialiser} objects, using:
     * - a {@link Map} of the type and format required for a {@link DataFlavour}
     * - a {@link Map} of the serialiser class and the domain class needed to create a {@link Serialiser}.
     *
     * @return a standard {@link StdSerialiserPrepopulationFactory} capable of building a {@link Serialiser} and {@link DataFlavour} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
    public StdSerialiserPrepopulationFactory serialiserPrepopulationFactory() {
        return new StdSerialiserPrepopulationFactory();
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
    DataService simpleDataService(final PersistenceLayer persistenceLayer,
                                  final DataReader dataReader) {
        return new SimpleDataService(persistenceLayer, dataReader);
    }

    @Bean
    AuditableDataService auditableDataService(final DataService dataService) {
        return new AuditableDataService(dataService);
    }

    @Bean
    AuditMessageService auditService(final Materializer materializer) {
        return new AuditMessageService(materializer);
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

    @Bean("threadPoolTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(CORE_POOL_SIZE);
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }
}
