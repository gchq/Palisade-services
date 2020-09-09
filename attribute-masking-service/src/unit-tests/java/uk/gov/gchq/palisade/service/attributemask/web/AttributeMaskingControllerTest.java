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

package uk.gov.gchq.palisade.service.attributemask.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.request.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

class AttributeMaskingControllerTest {

    AttributeMaskingService mockAttributeMaskingService = Mockito.mock(AttributeMaskingService.class);
    ErrorHandlingService mockErrorHandler = Mockito.mock(ErrorHandlingService.class);

    @Test
    void testControllerDelegatesToService() throws IOException {
        // given some test data
        AttributeMaskingController attributeMaskingController = new AttributeMaskingController(mockAttributeMaskingService, mockErrorHandler);
        Mockito.when(mockAttributeMaskingService.maskResourceAttributes(any())).thenReturn(ApplicationTestData.LEAF_RESOURCE);

        // when the controller is called
        attributeMaskingController.serviceMaskAttributes(
                ApplicationTestData.REQUEST_TOKEN,
                null,
                Optional.of(ApplicationTestData.REQUEST)
        );

        // then the service.storeAuthorisedRequest method is called
        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).storeAuthorisedRequest(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.USER,
                ApplicationTestData.LEAF_RESOURCE,
                ApplicationTestData.CONTEXT,
                ApplicationTestData.RULES
        );

        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).maskResourceAttributes(
                ApplicationTestData.LEAF_RESOURCE
        );
    }

    @Test
    void testStreamMarkerBypassesServiceDelegation() throws IOException {
        // given
        AttributeMaskingController attributeMaskingController = new AttributeMaskingController(mockAttributeMaskingService, mockErrorHandler);

        // when
        attributeMaskingController.serviceMaskAttributes(
                ApplicationTestData.REQUEST_TOKEN,
                StreamMarker.START_OF_STREAM,
                Optional.empty()
        );

        // then
        Mockito.verify(mockAttributeMaskingService, Mockito.never()).storeAuthorisedRequest(
                any(),
                any(),
                any(),
                any(),
                any()
        );

        // when
        attributeMaskingController.restMaskAttributes(
                ApplicationTestData.REQUEST_TOKEN,
                StreamMarker.END_OF_STREAM,
                Optional.empty()
        );

        // then
        Mockito.verify(mockAttributeMaskingService, Mockito.never()).storeAuthorisedRequest(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

}