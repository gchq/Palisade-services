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

package uk.gov.gchq.palisade.service.filteredresource.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetPersistenceLayer;

class OffsetEventServiceTest {

    @Test
    void topicOffsetServicePutsToPersistence() {
        // Given
        TokenOffsetPersistenceLayer persistenceLayer = Mockito.mock(TokenOffsetPersistenceLayer.class);
        OffsetEventService service = new OffsetEventService(persistenceLayer);

        // When
        service.storeTokenOffset("token", 1L);

        // Then
        Mockito.verify(persistenceLayer, Mockito.atLeastOnce()).putOffsetIfAbsent("token", 1L);
    }
}
