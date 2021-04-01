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

import uk.gov.gchq.palisade.service.filteredresource.ApplicationTestData;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.config.AsyncConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenOffsetEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, AsyncConfiguration.class, JpaTokenOffsetPersistenceLayer.class})
@EnableAutoConfiguration
@EntityScan(basePackages = "uk.gov.gchq.palisade.service.filteredresource.domain")
@EnableJpaRepositories(basePackages = "uk.gov.gchq.palisade.service.filteredresource.repository")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("db-test")
class JpaTokenOffsetPersistenceLayerTest {

    @Autowired
    private JpaTokenOffsetPersistenceLayer persistenceLayer;
    @Autowired
    private TokenOffsetRepository offsetsRepository;

    @Test
    void testContextLoads() {
        assertThat(persistenceLayer)
                .as("Check the persistenceLayer has been launched successfully ")
                .isNotNull();

        assertThat(offsetsRepository)
                .as("Check the repository has been started successfully")
                .isNotNull();
    }

    @Test
    void testPutAndGetReturnsExpectedEntity() {
        // given the persistence layer has something stored in it
        persistenceLayer.overwriteOffset(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.OFFSET
        ).join();

        // when all entities are retrieved from the repository
        Iterable<TokenOffsetEntity> authorisedRequests = offsetsRepository.findAll();

        // then the persistence layer has persisted the entity in the repository
        assertThat(authorisedRequests)
                .as("Check that the returned request contains only the correct information")
                .hasSize(1)
                .allMatch(requestEntity -> requestEntity.getToken().equals(ApplicationTestData.REQUEST_TOKEN))
                .allMatch(requestEntity -> requestEntity.getOffset().equals(ApplicationTestData.OFFSET));
    }
}
