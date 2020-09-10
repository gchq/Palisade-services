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

package uk.gov.gchq.palisade.component.attributemask.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData.PassThroughRule;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {AttributeMaskingApplication.class, ApplicationConfiguration.class, JpaPersistenceLayer.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("dbtest")
class JpaPersistenceLayerTest {
    // No DirtiesContext between methods as a restart is slow
    @Autowired
    private JpaPersistenceLayer persistenceLayer;
    @Autowired
    private AuthorisedRequestsRepository requestsRepository;


    @Test
    void testSpringDiscoversJpaPersistenceLayer() {
        // when the spring application is started
        // then the autowired persistence layer and repository are non-null
        assertThat(persistenceLayer).isNotNull();
        assertThat(requestsRepository).isNotNull();
    }

    @Test
    void testPutAndGetReturnsExpectedEntity() {
        // given the persistence layer has something stored in it
        persistenceLayer.put(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );

        // when all entities are retrieved from the repository
        Iterable<AuthorisedRequestEntity> authorisedRequests = requestsRepository.findAll();

        // then the persistence layer has persisted the entity in the repository
        assertThat(authorisedRequests)
                .hasSize(1)
                .allMatch(requestEntity -> requestEntity.getToken().equals(ApplicationTestData.REQUEST_TOKEN))
                .allMatch(requestEntity -> requestEntity.getResourceId().equals(ApplicationTestData.RESOURCE_ID))
                .allMatch(requestEntity -> requestEntity.getUser().equals(ApplicationTestData.USER))
                .allMatch(requestEntity -> requestEntity.getLeafResource().equals(ApplicationTestData.LEAF_RESOURCE))
                .allMatch(requestEntity -> requestEntity.getContext().equals(ApplicationTestData.CONTEXT))
                .allMatch(requestEntity -> requestEntity.getRules().getRules().get(ApplicationTestData.RULE_MESSAGE).getClass().equals(PassThroughRule.class));
    }
}
