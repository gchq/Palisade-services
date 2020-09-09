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

package uk.gov.gchq.palisade.component.attributemask.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.attributemask.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.web.AttributeMaskingController;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {AttributeMaskingApplication.class, ApplicationConfiguration.class, AttributeMaskingController.class})
class ControllerJpaComponentTest {

    @Autowired
    private AttributeMaskingController controller;
    @Autowired
    private AttributeMaskingService service;
    @Autowired
    private AuthorisedRequestsRepository repository;

    @Test
    void contextLoads() {
        // Given the spring boot application has been started

        // When various beans are autowired

        // Then the beans were found and wired successfully
        assertThat(controller).isNotNull();
        assertThat(service).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    void controllerPersistsAndMasks() throws IOException {
        // Given some application test data

        // When a request comes in to the controller
        Optional<LeafResource> maskedResource = controller.serviceMaskAttributes(ApplicationTestData.REQUEST_TOKEN, null, Optional.of(ApplicationTestData.REQUEST));

        // Then it is masked as per the service
        assertThat(maskedResource)
                .isPresent()
                .get()
                .isEqualTo(service.maskResourceAttributes(ApplicationTestData.LEAF_RESOURCE));

        assertThat(repository.findAll())
                .hasSize(1)
                .allSatisfy(entity -> assertThat(entity.getToken()).isEqualTo(ApplicationTestData.REQUEST_TOKEN))
                .allSatisfy(entity -> assertThat(entity.getUser()).isEqualTo(ApplicationTestData.USER))
                .allSatisfy(entity -> assertThat(entity.getLeafResource()).isEqualTo(ApplicationTestData.LEAF_RESOURCE))
                .allSatisfy(entity -> assertThat(entity.getContext()).isEqualTo(ApplicationTestData.CONTEXT))
                .allSatisfy(entity -> assertThat(entity.getRules()).isEqualTo(ApplicationTestData.RULES));
    }

}
