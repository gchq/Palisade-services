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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.data.repository.TestAsyncConfiguration;
import uk.gov.gchq.palisade.service.data.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.stream.config.AkkaSystemConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, TestAsyncConfiguration.class, AkkaSystemConfig.class})
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {AuthorisedRequestEntity.class})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.data.repository"})
@ActiveProfiles({"h2test", "test-serialisers"})
class DataSerialiserPrepopTest {
    @Autowired
    StdSerialiserConfiguration serdesConfig;

    @Value("${population.serialisers[0].flavourType}")
    String flavourType;

    @Test
    void testDataReaderIsPopulatedBySerialiser() {
        assertThat(flavourType)
                .isNotNull()
                .isEqualTo("java.lang.String");

        assertThat(serdesConfig)
                .extracting(StdSerialiserConfiguration::getSerialisers).asList()
                .hasSize(1);
    }
}
