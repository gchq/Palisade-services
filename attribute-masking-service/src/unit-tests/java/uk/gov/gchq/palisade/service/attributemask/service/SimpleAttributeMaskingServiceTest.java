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

package uk.gov.gchq.palisade.service.attributemask.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleAttributeMaskingServiceTest {

    JpaPersistenceLayer mockPersistenceLayer = Mockito.mock(JpaPersistenceLayer.class);

    @Test
    void testAttributeMaskingServiceDelegatesToPersistenceLayer() throws IOException {
        // given we have a simpleAttributeMaskingService with a mocked persistenceLayer
        AttributeMaskingService attributeMaskingService = new SimpleAttributeMaskingService(mockPersistenceLayer);

        // when we request to store some data
        attributeMaskingService.storeAuthorisedRequest(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );

        // then the persistence layer was requested to store the data
        Mockito.verify(mockPersistenceLayer, Mockito.atLeastOnce()).put(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );
    }

    @Test
    void testAttributeMaskingServiceMasksResource() {
        // given we have a simpleAttributeMaskingService with a mocked persistenceLayer
        AttributeMaskingService attributeMaskingService = new SimpleAttributeMaskingService(mockPersistenceLayer);

        // when we request to mask a resource
        LeafResource resource = attributeMaskingService.maskResourceAttributes(ApplicationTestData.LEAF_RESOURCE);

        // then the resource is masked appropriately
        // here, we expect nothing to have been done, but other implementations will apply some transformation on it
        assertThat(resource).isEqualTo(ApplicationTestData.LEAF_RESOURCE);
    }

}
