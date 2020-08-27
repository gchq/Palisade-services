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

package uk.gov.gchq.palisade.service.attributemask.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplicationTestData.PassThroughRule;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ApplicationConfiguration.class)
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
    void springDiscoversJpaPersistenceLayerTest() {
        // When the spring application is started
        // Then
        assertThat(persistenceLayer).isNotNull();
        assertThat(requestsRepository).isNotNull();
    }

    @Test
    @Transactional
    void putAndGetReturnsExpectedEntity() {
        // given
        persistenceLayer.put(
                AttributeMaskingApplicationTestData.REQUEST_TOKEN,
                AttributeMaskingApplicationTestData.USER,
                AttributeMaskingApplicationTestData.LEAF_RESOURCE,
                AttributeMaskingApplicationTestData.CONTEXT,
                AttributeMaskingApplicationTestData.RULES
        );

        // when
        Set<AuthorisedRequestEntity> authorisedRequests = StreamSupport.stream(requestsRepository.findAll().spliterator(), false).collect(Collectors.toSet());

        // then
        AuthorisedRequestEntity expected = new AuthorisedRequestEntity(
                AttributeMaskingApplicationTestData.REQUEST_TOKEN,
                AttributeMaskingApplicationTestData.USER,
                AttributeMaskingApplicationTestData.LEAF_RESOURCE,
                AttributeMaskingApplicationTestData.CONTEXT,
                AttributeMaskingApplicationTestData.RULES
        );
        assertThat(authorisedRequests)
                .hasSize(1)
                .allMatch(requestEntity -> requestEntity.getToken().equals(AttributeMaskingApplicationTestData.REQUEST_TOKEN))
                .allMatch(requestEntity -> requestEntity.getResourceId().equals(AttributeMaskingApplicationTestData.RESOURCE_ID))
                .allMatch(requestEntity -> requestEntity.getUser().equals(AttributeMaskingApplicationTestData.USER))
                .allMatch(requestEntity -> requestEntity.getLeafResource().equals(AttributeMaskingApplicationTestData.LEAF_RESOURCE))
                .allMatch(requestEntity -> requestEntity.getContext().equals(AttributeMaskingApplicationTestData.CONTEXT))
                .allMatch(requestEntity -> requestEntity.getRules().getRules().get(AttributeMaskingApplicationTestData.RULE_MESSAGE).getClass().equals(PassThroughRule.class));
    }
}
