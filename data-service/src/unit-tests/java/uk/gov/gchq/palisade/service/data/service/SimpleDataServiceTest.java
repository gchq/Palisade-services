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

package uk.gov.gchq.palisade.service.data.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.common.ResponseWriter;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.AUTHORISED_DATA_REQUEST;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.AUTHORISED_REQUEST_ENTITY;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.DATA_READER_REQUEST;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.DATA_READER_RESPONSE;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.DATA_REQUEST;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.TEST_RESPONSE_MESSAGE;

class SimpleDataServiceTest {

    public static final AtomicLong RECORDS_RETURNED = new AtomicLong(0);
    public static final AtomicLong RECORDS_PROCESSED = new AtomicLong(0);


    // Mocks
    final PersistenceLayer persistenceLayer = Mockito.mock(PersistenceLayer.class);
    final DataReader dataReader = Mockito.mock(DataReader.class);
    final SimpleDataService simpleDataService = new SimpleDataService(persistenceLayer, dataReader);


    /**
     * Test for {@link SimpleDataService#authoriseRequest(DataRequest)}.  If the request is found to be
     * authorised, the response will be the relevant information needed to proceed with the request. This will be in the
     * form of a {@code DataReaderRequest}.
     */
    @Test
    void testAuthoriseRequestWithAValidRequest() {
        // Given
        when(persistenceLayer.getAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(AUTHORISED_REQUEST_ENTITY)));

        // When
        CompletableFuture<AuthorisedDataRequest> authorisedDataRequestCompletableFuture = simpleDataService.authoriseRequest(DATA_REQUEST);

        // Then
        assertThat(authorisedDataRequestCompletableFuture
                .join())
                .as("")
                .usingRecursiveComparison()
                .isEqualTo(DATA_READER_REQUEST);

        //verifies the service calls PersistenceLayer getAsync method once
        verify(persistenceLayer, times(1)).getAsync(anyString(), anyString());
    }

    /**
     * Test for {@link SimpleDataService#authoriseRequest(DataRequest)}.  If the request is not valid/not authorised,
     * the method will throw an {@code ForbiddenException}
     */
    @Test
    void testAuthoriseRequestWithAnInvalidRequest() {
        // Given
        when(persistenceLayer.getAsync(any(), any()))
                .thenThrow(new ForbiddenException("test exception")); // temp dataRequest

        // When & Then
        assertThrows(ForbiddenException.class, () -> simpleDataService.authoriseRequest(DATA_REQUEST), "should throw UnauthorisedAccessException");

        //verifies the service calls PersistenceLayer getAsync method once
        verify(persistenceLayer, times(1)).getAsync(anyString(), anyString());
    }

    /**
     * Test for an authorised request,
     * the method will return the filtered requested data.
     * Note updating the AtomicLong object occurs in the dataReader which is mocked in this test.  Processing of
     * the OutputStream is done in the DataService and is used to verify the method is working as expected in this test.
     */
    @Test
    void testAuthoriseRequestWithARead() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Given
        when(dataReader.read(any(), any(), any())).thenReturn(DATA_READER_RESPONSE);

        // When
        CompletableFuture<Boolean> completed = simpleDataService
                .read(AUTHORISED_DATA_REQUEST, outputStream, RECORDS_PROCESSED, RECORDS_RETURNED);
        completed.join();
        String outputString = outputStream.toString();
        assertThat(outputString).isEqualTo(TEST_RESPONSE_MESSAGE);

        //verifies the service calls DataReader read method once
        verify(dataReader, times(1)).read(any(), any(), any());

    }
}
