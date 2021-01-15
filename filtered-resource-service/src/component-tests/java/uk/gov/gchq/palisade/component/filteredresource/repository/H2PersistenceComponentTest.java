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

package uk.gov.gchq.palisade.component.filteredresource.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.contract.filteredresource.common.ContractTestData;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.config.AsyncConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetRepository;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, AsyncConfiguration.class, JpaTokenOffsetPersistenceLayer.class})
@EnableAutoConfiguration
@EntityScan(basePackages = "uk.gov.gchq.palisade.service.filteredresource.domain")
@EnableJpaRepositories(basePackages = "uk.gov.gchq.palisade.service.filteredresource.repository")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("h2")
class H2PersistenceComponentTest {

    @Autowired
    private OffsetEventService service;

    @Autowired
    private TokenOffsetRepository tokenOffsetRepository;

    @Test
    void testContextLoads() {
        assertThat(service).isNotNull();
        assertThat(tokenOffsetRepository).isNotNull();
    }

    @Test
    void testTopicOffsetsAreStoredInH2() {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.queuePointer).join();

        // Then the offset is persisted in redis
        assertThat(tokenOffsetRepository.findByToken(token)).isPresent();
    }

}
