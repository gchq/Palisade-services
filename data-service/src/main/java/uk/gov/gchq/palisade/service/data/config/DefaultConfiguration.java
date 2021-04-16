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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.data.common.data.DataFlavour;
import uk.gov.gchq.palisade.service.data.common.data.DataService;
import uk.gov.gchq.palisade.service.data.common.data.reader.DataReader;
import uk.gov.gchq.palisade.service.data.common.data.seralise.Serialiser;
import uk.gov.gchq.palisade.service.data.reader.SimpleDataReader;
import uk.gov.gchq.palisade.service.data.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;

import java.util.Map;

/**
 * Overridable beans for the data-service.
 * Additional classpath-jars may override these for implementation-specific behaviour.
 */
@Configuration
public class DefaultConfiguration {

    /**
     * Bean for a {@link SimpleDataService}, connecting a {@link DataReader} and {@link PersistenceLayer}.
     * These are likely the {@code HadoopDataReader} and the {@link JpaPersistenceLayer}.
     *
     * @param persistenceLayer the persistence layer for reading authorised requests
     * @param dataReader       the data reader to use for reading resource data from storage
     * @return a new {@link SimpleDataService}
     */
    @Bean
    @ConditionalOnProperty(prefix = "data.service", name = "implementation", havingValue = "simple", matchIfMissing = true)
    DataService simpleDataService(final PersistenceLayer persistenceLayer,
                                  final DataReader dataReader) {
        return new SimpleDataService(persistenceLayer, dataReader);
    }

    /**
     * Bean for a {@link SimpleDataReader}, this simply reads from the local filesystem.
     *
     * @return a new {@link SimpleDataReader}
     */
    @Bean
    @ConditionalOnProperty(prefix = "data.reader", name = "implementation", havingValue = "simple", matchIfMissing = true)
    DataReader simpleDataReader() {
        return new SimpleDataReader();
    }

    /**
     * A {@link StdSerialiserConfiguration} object that uses Spring to configure a list of serialisers from a yaml file
     * A container for a number of {@link StdSerialiserPrepopulationFactory} builders used for creating {@link Serialiser}s
     * These serialisers will be used for pre-populating the {@link DataService}
     *
     * @return a {@link StdSerialiserConfiguration} containing a list of {@link StdSerialiserPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    StdSerialiserConfiguration serialiserConfiguration() {
        return new StdSerialiserConfiguration();
    }

    /**
     * Implementation of a {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file
     * A factory for {@link Serialiser} objects, using:
     * - a {@link Map} of the type and format required for a {@link DataFlavour}
     * - a {@link Map} of the serialiser class and the domain class needed to create a {@link Serialiser}
     *
     * @return a standard {@link StdSerialiserPrepopulationFactory} capable of building a {@link Serialiser} and {@link DataFlavour} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
    StdSerialiserPrepopulationFactory serialiserPrepopulationFactory() {
        return new StdSerialiserPrepopulationFactory();
    }
}
