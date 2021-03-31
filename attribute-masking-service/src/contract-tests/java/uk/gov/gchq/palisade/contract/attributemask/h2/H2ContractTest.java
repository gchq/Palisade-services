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

package uk.gov.gchq.palisade.contract.attributemask.h2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.contract.attributemask.ContractTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.User;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An external requirement of the service is to write to an external persistence store.
 * This store will then be read by the data-service to apply rules for a data-read.
 * Upon storing an authorised request with the service, an external entity should be able to retrieve it.
 * The key distinction between this and the JpaPersistenceLayerTest is while the aforementioned inspects
 * its own instance of the database, this test creates two distinct, separate connections.
 */
@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableJpaRepositories(basePackageClasses = {AuthorisedRequestsRepositoryExternalConnection.class, AuthorisedRequestsRepository.class})
@ActiveProfiles({"db-test", "akka"})
class H2ContractTest {

    @Autowired
    private AttributeMaskingService attributeMaskingService;

    @Autowired
    private AuthorisedRequestsRepositoryExternalConnection externalRepositoryConnection;

    @Test
    void testContextLoads() {
        assertThat(attributeMaskingService)
                .as("Check that the attributeMaskingService ahs been autowired successfully")
                .isNotNull();

        assertThat(externalRepositoryConnection)
                .as("Check that the externalRepositoryConnection has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testStoredAuthorisedRequestsAreRetrievableExternally() {
        // Given a request is stored by the service
        attributeMaskingService.storeAuthorisedRequest(
                ContractTestData.REQUEST_TOKEN,
                ContractTestData.REQUEST_OBJ
        ).join();

        // When the "data-service" (this test class) retrieves this stored request
        var persistedEntity = externalRepositoryConnection.findByTokenAndResourceId(ContractTestData.REQUEST_TOKEN, ContractTestData.RESOURCE_ID);

        assertThat(persistedEntity).get()
                .as("Check after extracting individual fields from the entity, that they have been persisted correctly")
                .extracting("resourceId", "user", "context")
                .contains(ContractTestData.RESOURCE_ID, new User().userId(ContractTestData.USER_ID), new Context().purpose(ContractTestData.PURPOSE));

    }

}
