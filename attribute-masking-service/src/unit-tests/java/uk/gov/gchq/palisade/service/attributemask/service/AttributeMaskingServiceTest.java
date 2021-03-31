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

package uk.gov.gchq.palisade.service.attributemask.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.User;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class AttributeMaskingServiceTest {

    public static class IdentityMasker implements LeafResourceMasker {
        @Override
        public LeafResource apply(final LeafResource resource) {
            return resource;
        }
    }

    JpaPersistenceLayer mockPersistenceLayer = Mockito.mock(JpaPersistenceLayer.class);
    LeafResourceMasker spyMasker = Mockito.spy(new IdentityMasker());
    AttributeMaskingService attributeMaskingService = new AttributeMaskingService(mockPersistenceLayer, spyMasker);

    @AfterEach
    void tearDown() {
        Mockito.reset(mockPersistenceLayer, spyMasker);
    }

    @Test
    void testAttributeMaskingServiceDelegatesToPersistenceLayer() {
        // given we have a simpleAttributeMaskingService with a mocked persistenceLayer
        Mockito.when(mockPersistenceLayer.putAsync(anyString(), any(User.class), any(LeafResource.class), any(Context.class), any(Rules.class)))
                .thenReturn(CompletableFuture.completedFuture(ApplicationTestData.REQUEST));
        // when we request to store some data
        attributeMaskingService.storeAuthorisedRequest(ApplicationTestData.REQUEST_TOKEN, ApplicationTestData.REQUEST);

        // then the persistence layer was requested to store the data
        Mockito.verify(mockPersistenceLayer, Mockito.atLeastOnce()).putAsync(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );
    }

    @Test
    void testAttributeMaskingServiceStoreIgnoresNulls() {
        // given we have a simpleAttributeMaskingService with a mocked persistenceLayer

        // when we request to store a null
        attributeMaskingService.storeAuthorisedRequest(ApplicationTestData.REQUEST_TOKEN, null);

        // then it did not request to store with the persistence layer
        Mockito.verify(mockPersistenceLayer, Mockito.never()).putAsync(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testAttributeMaskingServiceMasksResource() {
        // given we have a simpleAttributeMaskingService with a mocked resourceMasker

        // when we request to mask a resource
        attributeMaskingService.maskResourceAttributes(ApplicationTestData.REQUEST);

        // then the leaf resource masker was called
        Mockito.verify(spyMasker, Mockito.atLeastOnce()).apply(ApplicationTestData.LEAF_RESOURCE);
    }

    @Test
    void testAttributeMaskingServiceMaskAllowsNulls() {
        // given we have a simpleAttributeMaskingService with a mocked resourceMasker

        // when we request to mask a resource
        attributeMaskingService.maskResourceAttributes(null);

        // then the leaf resource masker was called
        Mockito.verify(spyMasker, Mockito.never()).apply(any());
    }

}
