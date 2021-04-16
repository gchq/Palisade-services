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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.data.common.data.reader.DataReader;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.RECORDS_PROCESSED;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.RECORDS_RETURNED;
import static uk.gov.gchq.palisade.service.data.DataServiceTestsCommon.TEST_RESPONSE_MESSAGE;

class SimpleDataServiceTest {


    //mocks
    final PersistenceLayer persistenceLayer = Mockito.mock(PersistenceLayer.class);
    final DataReader dataReader = Mockito.mock(DataReader.class);

    private SimpleDataService simpleDataService;

    @BeforeEach
    void setUp() {
        simpleDataService = new SimpleDataService(persistenceLayer, dataReader);
    }

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

        // When & Then
        assertThat(simpleDataService.authoriseRequest(DATA_REQUEST)
                .join())
                .as("Check authoriseRequest returns a DataReaderRequest")
                .usingRecursiveComparison()
                .isEqualTo(DATA_READER_REQUEST);

        //verifies the service calls the PersistenceLayer getAsync method once
        verify(persistenceLayer, times(1)).getAsync(anyString(), anyString());
    }

    /**
     * Test for {@link SimpleDataService#authoriseRequest(DataRequest)} when no data is returned from the persistence
     * storage.  The expected response will be for the method to throw a {@link ForbiddenException}.
     */
    @Test
    void testAuthoriseRequestWithAnInvalidRequest() {

        //same message as the one provided by the ForbiddenException
        String errorMessage = "There is no data for the request, with token %s and resource %s";

        // Given
        when(persistenceLayer.getAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        //When
        Throwable thrown = catchThrowable(() -> simpleDataService.authoriseRequest(DATA_REQUEST).join());

        //Then
        assertThat(thrown)
                .as("Check that invalid request to authoriseRequest will throw a ForbiddenException")
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ForbiddenException.class)
                .hasMessageContaining(errorMessage, DATA_REQUEST.getToken(), DATA_REQUEST.getLeafResourceId());

        //verifies the service calls the PersistenceLayer getAsync method once
        verify(persistenceLayer, times(1)).getAsync(anyString(), anyString());
    }

    /**
     * Test for {@link SimpleDataService#read(AuthorisedDataRequest, OutputStream, AtomicLong, AtomicLong)}.
     * The method will return {@code OutputStream} linked to the input provided by the {@code DataReader} and supply
     * the requested data.
     */
    @Test
    void testAuthoriseRequestWithARead() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Given
        when(dataReader.read(any(), any(), any())).thenReturn(DATA_READER_RESPONSE);

        // When
        simpleDataService
                .read(AUTHORISED_DATA_REQUEST, outputStream, RECORDS_PROCESSED, RECORDS_RETURNED)
                .join();

        //Then
        String outputString = outputStream.toString();
        assertThat(outputString)
                .as("Check that read will provide data in the output stream")
                .isEqualTo(TEST_RESPONSE_MESSAGE);

        //verifies the service calls the DataReader read method once
        verify(dataReader, times(1)).read(any(), any(), any());

    }
}
