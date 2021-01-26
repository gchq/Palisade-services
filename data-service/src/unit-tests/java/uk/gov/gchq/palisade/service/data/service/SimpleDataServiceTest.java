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

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.gchq.palisade.service.data.model.DataReaderRequestModel;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;
import uk.gov.gchq.palisade.service.data.repository.PersistenceLayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class SimpleDataServiceTest {

    public static final String REQUEST_TOKEN = "test-request-token";

    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final User USER = new User().userId(USER_ID);
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String RESOURCE_TYPE = "uk.gov.gchq.palisade.test.TestType";
    public static final String RESOURCE_FORMAT = "avro";
    public static final String DATA_SERVICE_NAME = "test-data-service";
    public static final String RESOURCE_PARENT = "/test";
    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id(RESOURCE_ID)
            .type(RESOURCE_TYPE)
            .serialisedFormat(RESOURCE_FORMAT)
            .connectionDetail(new SimpleConnectionDetail().serviceName(DATA_SERVICE_NAME))
            .parent(new SystemResource().id(RESOURCE_PARENT));

    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);

    public static final String RULE_MESSAGE = "test-rule";

    public static final AtomicLong recordsReturned = new AtomicLong(0);
    public static final AtomicLong recordsProcessed = new AtomicLong(0);


    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }

    public static final Rules<Serializable> RULES = new Rules<Serializable>().addRule(RULE_MESSAGE, new PassThroughRule<Serializable>());

    // Mocks
    final PersistenceLayer persistenceLayer = Mockito.mock(PersistenceLayer.class);
    final DataReader dataReader = Mockito.mock(DataReader.class);
    final SimpleDataService simpleDataService = new SimpleDataService(persistenceLayer, dataReader);


    // Test data
    final DataRequestModel dataRequestModel = DataRequestModel.Builder.create().withToken(REQUEST_TOKEN).withLeafResourceId(RESOURCE_ID);
    final AuthorisedRequestEntity authorisedEntity = new AuthorisedRequestEntity(
            REQUEST_TOKEN,
            USER,
            LEAF_RESOURCE,
            CONTEXT,
            RULES
    );

    final DataReaderRequest readerRequest = new DataReaderRequest()
            .user(USER)
            .resource(LEAF_RESOURCE)
            .context(CONTEXT)
            .rules(RULES);

    final DataReaderRequestModel dataReaderRequestModel = DataReaderRequestModel.Builder.create()
            .withResource(LEAF_RESOURCE)
            .withUser(USER)
            .withContext(CONTEXT)
            .withRules(RULES);

    final String testResponseMessage = "test response for data request";

    final ResponseWriter responseWriter = new ResponseWriter() {
        String testData = testResponseMessage;

        @Override
        public void close() throws IOException {

        }

        @Override
        public ResponseWriter write(final OutputStream outputStream) throws IOException {

            InputStream testInputStream = new ByteArrayInputStream(testData.getBytes());
            testInputStream.transferTo(outputStream);
            testInputStream.close();
            return this;
        }
    };

    final ResponseWriter errorProneResponseWriter = new ResponseWriter() {
        String testData = testResponseMessage;

        @Override
        public void close() throws IOException {

        }

        @Override
        public ResponseWriter write(final OutputStream outputStream) throws IOException {
            throw new IOException("Something went wrong");

        }
    };

    final DataReaderResponse dataReaderResponse = new DataReaderResponse()
            .message("test message")
            .writer(responseWriter);

    final DataReaderResponse errorDataReaderResponse = new DataReaderResponse()
            .message("test message")
            .writer(errorProneResponseWriter);

    /**
     * Test for {@link SimpleDataService#authoriseRequest(DataRequestModel)}.  If the request is found to be
     * authorised, the response will be the relevant information needed to proceed with the request. This will be in the
     * form of a {@code DataReaderRequest}.
     */
    @Test
    void testAuthoriseRequestWithAValidRequest() {

        //given
        when(persistenceLayer.getAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(authorisedEntity)));


        //when
        CompletableFuture<DataReaderRequestModel> dataReaderRequestCompletableFuture = simpleDataService.authoriseRequest(dataRequestModel);

        //then
        assertThat(dataReaderRequestCompletableFuture.join())
                .usingRecursiveComparison()
                .isEqualTo(readerRequest);
        verify(persistenceLayer, times(1)).getAsync(anyString(), anyString());


    }

    /**
     * Test for {@link SimpleDataService#authoriseRequest(DataRequestModel)}.  If the request is not valid/not authorised,
     * the method will throw an {@code UnauthorisedAccessException}
     */
    @Test
    void testAuthoriseRequestWithAnInvalidRequest() {

        //given
        when(persistenceLayer.getAsync(any(), any()))
                .thenThrow(new ForbiddenException("test exception")); // temp dataRequest

        // when & then
        Exception unauthorisedAccessException = assertThrows(ForbiddenException.class, () -> simpleDataService.authoriseRequest(dataRequestModel), "should throw UnauthorisedAccessException");
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


        //given
        when(dataReader.read(any(), any(), any())).thenReturn(dataReaderResponse);

        //when
        CompletableFuture<Boolean> completed = simpleDataService
                .read(dataReaderRequestModel, outputStream, recordsProcessed, recordsReturned);
        completed.join();
        String outputString = new String(outputStream.toByteArray());
        assertThat(outputString).isEqualTo(testResponseMessage);
        verify(dataReader, times(1)).read(any(), any(), any());

    }
}
