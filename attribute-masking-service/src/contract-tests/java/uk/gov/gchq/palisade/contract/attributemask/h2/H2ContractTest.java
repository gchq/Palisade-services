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

package uk.gov.gchq.palisade.contract.attributemask.h2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An external requirement of the service is to write to an external persistence store.
 * This store will then be read by the data-service to apply rules for a data-read.
 * Upon storing an authorised request with the service, an external entity should be able to retrieve it.
 *
 * The key distinction between this and the JpaPersistenceLayerTest is while the aforementioned inspects
 * its own instance of the database, this test creates two distinct, separate connections.
 */
@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableJpaRepositories(basePackageClasses = {AuthorisedRequestsRepositoryExternalConnection.class, AuthorisedRequestsRepository.class})
@ActiveProfiles("dbtest")
class H2ContractTest {

    @Autowired
    private AttributeMaskingService attributeMaskingService;

    @Autowired
    private AuthorisedRequestsRepositoryExternalConnection externalRepositoryConnection;

    @Test
    void contextLoads() {
        assertThat(attributeMaskingService).isNotNull();
        assertThat(externalRepositoryConnection).isNotNull();
    }

    @Test
    void storedAuthorisedRequestsAreRetrievableExternally() throws IOException {
        // Given a request is stored by the service
        attributeMaskingService.storeAuthorisedRequest(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );

        // When the "data-service" (this test class) retrieves this stored request
        Optional<AuthorisedRequestEntity> persistedEntity = externalRepositoryConnection.findByTokenAndResourceId(ApplicationTestData.REQUEST_TOKEN, ApplicationTestData.RESOURCE_ID);

        // Then an entity is found and it is equivalent to the request stored
        assertThat(persistedEntity).isPresent();
        assertThat(persistedEntity).get()
                .isEqualTo(new AuthorisedRequestEntity(
                        ApplicationTestData.REQUEST_TOKEN,
                        ApplicationTestData.USER,
                        ApplicationTestData.LEAF_RESOURCE,
                        ApplicationTestData.CONTEXT,
                        ApplicationTestData.RULES));
    }

}
