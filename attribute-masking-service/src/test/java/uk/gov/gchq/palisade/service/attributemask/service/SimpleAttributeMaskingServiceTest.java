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

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

class SimpleAttributeMaskingServiceTest {

    JpaPersistenceLayer mockPersistenceLayer = Mockito.mock(JpaPersistenceLayer.class);

    @Test
    void queryScopeServiceDelegatesToPersistenceLayer() {
        // given
        AttributeMaskingService attributeMaskingService = new SimpleAttributeMaskingService(mockPersistenceLayer);

        // when
        attributeMaskingService.storeRequestResult(
                AttributeMaskingApplicationTestData.REQUEST_TOKEN,
                AttributeMaskingApplicationTestData.USER,
                AttributeMaskingApplicationTestData.LEAF_RESOURCE,
                AttributeMaskingApplicationTestData.CONTEXT,
                AttributeMaskingApplicationTestData.RULES
        );

        Mockito.verify(mockPersistenceLayer, Mockito.atLeastOnce()).put(
                AttributeMaskingApplicationTestData.REQUEST_TOKEN,
                AttributeMaskingApplicationTestData.USER,
                AttributeMaskingApplicationTestData.LEAF_RESOURCE,
                AttributeMaskingApplicationTestData.CONTEXT,
                AttributeMaskingApplicationTestData.RULES
        );
    }

}
