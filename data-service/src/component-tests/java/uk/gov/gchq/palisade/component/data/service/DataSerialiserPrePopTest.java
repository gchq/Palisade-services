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

package uk.gov.gchq.palisade.component.data.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.data.service.DataSerialiserPrePopTest.SerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@SpringBootTest
@ContextConfiguration(classes = {SerialiserConfiguration.class})
@ActiveProfiles({"test-serialisers"})
// Fails to consistently work for unknown reasons
@Disabled
class DataSerialiserPrePopTest {

    @Configuration
    public static class SerialiserConfiguration {
        @Bean
        @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
        @ConfigurationProperties(prefix = "population")
        StdSerialiserConfiguration serialiserConfiguration() {
            return new StdSerialiserConfiguration();
        }

        @Bean
        @ConditionalOnProperty(prefix = "population", name = "serialiserProvider", havingValue = "std", matchIfMissing = true)
        StdSerialiserPrepopulationFactory serialiserPrepopulationFactory() {
            return new StdSerialiserPrepopulationFactory();
        }
    }

    @Autowired
    StdSerialiserConfiguration serdesConfig;

    @Value("${population.serialisers[0].flavourType}")
    String flavourType;
    @Value("${population.serialisers[0].flavourFormat}")
    String flavourFormat;
    @Value("${population.serialisers[0].serialiserClass}")
    String serialiserClass;

    @Test
    void testDataReaderIsPopulatedBySerialiser() {
        // Given the SpringBoot context has loaded and read the config yaml

        // Given we expect the prepop factory to serialise "string"/"java.lang.String" type/format resources with the TestSerialiser
        var std = new StdSerialiserPrepopulationFactory();
        std.setFlavourFormat("string");
        std.setFlavourType("java.lang.String");
        std.setSerialiserClass("uk.gov.gchq.palisade.component.data.service.TestSerialiser");

        // Check that the yaml contains the expected values before we do any tests
        assumeThat(flavourType)
                .as("The flavourType should be read from config")
                .isNotNull()
                .isEqualTo("java.lang.String");
        assumeThat(flavourFormat)
                .as("The flavourFormat should be read from config")
                .isNotNull()
                .isEqualTo("string");
        assumeThat(serialiserClass)
                .as("The serialiserClass should be read from config")
                .isNotNull()
                .isEqualTo("uk.gov.gchq.palisade.component.data.service.TestSerialiser");

        // When the serdesConfig is autowired
        // Then it should produce the expected prepop factory
        assertThat(serdesConfig)
                .extracting(StdSerialiserConfiguration::getSerialisers)
                .asList()
                .as("There should be one configured serialiser")
                .hasSize(1)
                .first()
                .usingRecursiveComparison()
                .as("The configured serialiser should match the expected")
                .isEqualTo(std);
    }
}
