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

package uk.gov.gchq.palisade.service.data;

import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.common.ResponseWriter;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.User;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.reader.common.rule.Rule;
import uk.gov.gchq.palisade.reader.common.rule.Rules;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class DataServiceTestsCommon {

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final User USER = new User().userId("test-user-id");
    public static final String RESOURCE_ID = "/test/resourceId";

    public static final LeafResource LEAF_RESOURCE = new FileResource()
            .id(RESOURCE_ID)
            .type("uk.gov.gchq.palisade.test.TestType")
            .serialisedFormat("avro")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-data-service"))
            .parent(new SystemResource().id("/test"));

    public static final Context CONTEXT = new Context().purpose("test-purpose");
    public static final String RULE_MESSAGE = "test-rule";

    public static final Rules<Serializable> RULES = new Rules<>()
            .addRule(RULE_MESSAGE, new PassThroughRule<>());

    public static final AtomicLong RECORDS_RETURNED = new AtomicLong(0);
    public static final AtomicLong RECORDS_PROCESSED = new AtomicLong(0);

    public static final DataRequest DATA_REQUEST = DataRequest.Builder.create()
            .withToken(REQUEST_TOKEN)
            .withLeafResourceId(RESOURCE_ID);

    public static final AuthorisedRequestEntity AUTHORISED_REQUEST_ENTITY = new AuthorisedRequestEntity(
            REQUEST_TOKEN,
            USER,
            LEAF_RESOURCE,
            CONTEXT,
            RULES
    );

    public static final DataReaderRequest DATA_READER_REQUEST = new DataReaderRequest()
            .user(USER)
            .resource(LEAF_RESOURCE)
            .context(CONTEXT)
            .rules(RULES);

    public static final AuthorisedDataRequest AUTHORISED_DATA_REQUEST = AuthorisedDataRequest.Builder.create()
            .withResource(LEAF_RESOURCE)
            .withUser(USER)
            .withContext(CONTEXT)
            .withRules(RULES);

    public static final String TEST_RESPONSE_MESSAGE = "test response for data request";

    public static final ResponseWriter RESPONSE_WRITER = new ResponseWriter() {
        final String testData = TEST_RESPONSE_MESSAGE;

        @Override
        public void close() {
        }

        @Override
        public ResponseWriter write(final OutputStream outputStream) throws IOException {
            try (var testInputStream = new ByteArrayInputStream(testData.getBytes())) {
                testInputStream.transferTo(outputStream);
            }
            return this;
        }
    };

    public static final DataReaderResponse DATA_READER_RESPONSE = new DataReaderResponse()
            .message("test message")
            .writer(RESPONSE_WRITER);

    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }

        @Override
        public boolean isApplicable(final User user, final Context context) {
            return false; // rules are not applicable
        }
    }
}
