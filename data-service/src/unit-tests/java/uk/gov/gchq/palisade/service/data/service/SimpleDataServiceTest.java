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

package uk.gov.gchq.palisade.service.data.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.common.ResponseWriter;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleDataServiceTest {
    // Mocks
    final PersistenceLayer persistenceLayer = Mockito.mock(PersistenceLayer.class);
    final DataReader dataReader = Mockito.mock(DataReader.class);
    final DataReaderResponse readerResponse = Mockito.mock(DataReaderResponse.class);
    final SimpleDataService simpleDataService = new SimpleDataService(persistenceLayer, dataReader);
    final OutputStream outputStream = Mockito.mock(OutputStream.class);

    // Test data
    final DataRequest dataRequest = DataRequest.Builder.create().withToken("test-request-token").withLeafResourceId("/resource/id");
    final AuthorisedRequestEntity entity = new AuthorisedRequestEntity(
            "test-request-token",
            new User().userId("user-id"),
            new FileResource().id("/resource/id"),
            new Context(),
            new Rules<>()
    );
    final DataReaderRequest readerRequest = new DataReaderRequest()
            .user(entity.getUser())
            .resource(entity.getLeafResource())
            .context(entity.getContext())
            .rules(entity.getRules());

    @Test
    void testAuthoriseRequestDelegatesToPersistence() {
        // Given the persistence layer is mocked to return the test data
        Mockito.when(persistenceLayer.getAsync(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(entity)));

        // When
        CompletableFuture<Optional<DataReaderRequest>> authorisedRequest = simpleDataService.authoriseRequest(dataRequest);

        // Then the mock has been called
        Mockito.verify(persistenceLayer, Mockito.atLeastOnce()).getAsync(Mockito.anyString(), Mockito.anyString());

        // Then the request was authorised
        assertThat(authorisedRequest.join())
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .isEqualTo(readerRequest);
    }

    @Test
    void testReadDelegatesToReader() {
        // Given the reader is mocked to return the test data
        Mockito.when(dataReader.read(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(readerResponse);
        Mockito.when(readerResponse.getWriter())
                .thenReturn(Mockito.mock(ResponseWriter.class));

        // When
        Pair<AtomicLong, AtomicLong> recordsReadAndReturned = simpleDataService.read(readerRequest, outputStream);

        // Then the mock has been called
        Mockito.verify(dataReader, Mockito.atLeastOnce()).read(Mockito.any(), Mockito.any(), Mockito.any());

        // Then the correct number of records was (not) read
        assertThat(recordsReadAndReturned).isEqualTo(Pair.of(0L, 0L));
    }
}
