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

package uk.gov.gchq.palisade.component.attributemask.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.attributemask.PassThroughRule;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.CONTEXT;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.LEAF_RESOURCE;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.REQUEST_TOKEN;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.RESOURCE_ID;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.RULES;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.RULE_MESSAGE;
import static uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.USER;

@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, ExecutorTestConfiguration.class})
@EntityScan(basePackageClasses = {AuthorisedRequestEntity.class})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.attributemask.repository"})
class JpaPersistenceLayerTest {

    @Autowired
    private JpaPersistenceLayer persistenceLayer;
    @Autowired
    private AuthorisedRequestsRepository requestsRepository;

    @Test
    void testContextLoads() {
        assertThat(persistenceLayer)
                .as("Check that the persistenceLayer has been autowired successfully")
                .isNotNull();

        assertThat(requestsRepository)
                .as("Check that the requestsRepository has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testPutAndGetReturnsExpectedEntity() {
        // given the persistence layer has something stored in it
        persistenceLayer.putAsync(
                REQUEST_TOKEN,
                USER,
                LEAF_RESOURCE,
                CONTEXT,
                RULES
        ).join();

        // when all entities are retrieved from the repository
        var authorisedRequests = requestsRepository.findAll();

        // then the persistence layer has persisted the entity in the repository
        assertThat(authorisedRequests)
                .as("Check that there is one entity returned")
                .hasSize(1)
                .allSatisfy(requestEntity -> {
                    assertThat(requestEntity)
                            .as("Check that by extracting the objects from the entity, they are the correct ones")
                            .extracting("token", "resourceId", "user", "leafResource", "context")
                            .contains(REQUEST_TOKEN, RESOURCE_ID, USER, LEAF_RESOURCE, CONTEXT);

                    assertThat(requestEntity.getRules().getRules())
                            .as("Check that the rules map contains the correct rule")
                            .containsKeys(RULE_MESSAGE)
                            .as("Check that the rule in the map is a PassThroughRule")
                            .extractingByKey(RULE_MESSAGE)
                            .isInstanceOf(PassThroughRule.class);
                });
    }
}
