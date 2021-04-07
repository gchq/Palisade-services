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

import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.data.seralise.Serialiser;

import java.util.Map;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class PrepopulationConfiguration {

    /**
     * A {@link StdSerialiserConfiguration} object that uses Spring to configure a list of serialisers from a yaml file
     * A container for a number of {@link StdSerialiserPrepopulationFactory} builders used for creating {@link Serialiser}s
     * These serialisers will be used for pre-populating the {@link uk.gov.gchq.palisade.service.data.service.DataService}
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
     * Implementation of a {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file
     * A factory for {@link Serialiser} objects, using:
     * - a {@link Map} of the type and format required for a {@link DataFlavour}
     * - a {@link Map} of the serialiser class and the domain class needed to create a {@link Serialiser}
     *
     * @return a standard {@link StdSerialiserPrepopulationFactory} capable of building a {@link Serialiser} and {@link DataFlavour} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
    public StdSerialiserPrepopulationFactory serialiserPrepopulationFactory() {
        return new StdSerialiserPrepopulationFactory();
    }
}
