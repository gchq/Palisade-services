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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.model.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.model.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

class AttributeMaskingRestControllerTest {

    AttributeMaskingService mockAttributeMaskingService = Mockito.mock(AttributeMaskingService.class);

    @AfterEach
    void tearDown() {
        Mockito.reset(mockAttributeMaskingService);
    }

    @Test
    void testControllerDelegatesToService() {
        // given some test data, and a mocked service behind the controller
        AttributeMaskingRestController attributeMaskingRestController = new AttributeMaskingRestController(mockAttributeMaskingService);
        Mockito.when(mockAttributeMaskingService.maskResourceAttributes(any()))
                .thenReturn(ApplicationTestData.RESPONSE);
        Mockito.when(mockAttributeMaskingService.storeAuthorisedRequest(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // when the controller is called with a request

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(
                Collections.singletonMap(Token.HEADER, Collections.singletonList(ApplicationTestData.REQUEST_TOKEN)));
        attributeMaskingRestController.maskAttributes(
                headers,
                ApplicationTestData.REQUEST
        );

        // then the service.storeAuthorisedRequest method is called
        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).storeAuthorisedRequest(
                ApplicationTestData.REQUEST_TOKEN,
                ApplicationTestData.REQUEST
        );

        // then the service.maskResourceAttributes method is called
        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).maskResourceAttributes(
                ApplicationTestData.REQUEST
        );
    }

    @Test
    void testControllerHandlesNullRequests() {
        // given some test data, and a mocked service behind the controller
        AttributeMaskingRestController attributeMaskingRestController = new AttributeMaskingRestController(mockAttributeMaskingService);
        Mockito.when(mockAttributeMaskingService.storeAuthorisedRequest(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // when the controller is called with a stream marker
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(
                Stream.of(
                        new SimpleImmutableEntry<>(Token.HEADER, Collections.singletonList(ApplicationTestData.REQUEST_TOKEN)),
                        new SimpleImmutableEntry<>(StreamMarker.HEADER, Collections.singletonList(StreamMarker.START.toString())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        attributeMaskingRestController.maskAttributes(
                headers,
                null
        );

        // then the storeAuthorisedRequest is called
        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).storeAuthorisedRequest(
                any(),
                any()
        );

        // then the maskResourceAttributes is called
        Mockito.verify(mockAttributeMaskingService, Mockito.atLeastOnce()).maskResourceAttributes(
                any()
        );
    }

}