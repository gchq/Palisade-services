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

package uk.gov.gchq.palisade.service.attributemask;

import org.apache.kafka.clients.producer.ProducerRecord;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Token;
import uk.gov.gchq.palisade.service.attributemask.common.User;
import uk.gov.gchq.palisade.service.attributemask.common.UserId;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rule;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.model.StreamMarker;

import java.io.Serializable;

public class ApplicationTestData {
    /**
     * Common test data for all classes
     */

    private ApplicationTestData() {
        // hide the constructor, this is just a collection of static objects
    }

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

    public static final Context CONTEXT = new Context().purpose("test-purpose");

    public static final String RULE_MESSAGE = "test-rule";

    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }

    public static final Rules<Serializable> RULES = new Rules<>().addRule(RULE_MESSAGE, new PassThroughRule<>());

    public static final AttributeMaskingRequest REQUEST = AttributeMaskingRequest.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withUser(USER)
            .withResource(LEAF_RESOURCE)
            .withRules(RULES);

    public static final AttributeMaskingResponse RESPONSE = AttributeMaskingResponse.Builder.create(REQUEST)
            .withResource(LEAF_RESOURCE);

    public static final ProducerRecord<String, AttributeMaskingRequest> START = new ProducerRecord<>("rule", 0, null, null);
    public static final ProducerRecord<String, AttributeMaskingRequest> RECORD = new ProducerRecord<>("rule", 0, null, REQUEST);
    public static final ProducerRecord<String, AttributeMaskingRequest> END = new ProducerRecord<>("rule", 0, null, null);

    static {
        START.headers().add(StreamMarker.HEADER, StreamMarker.START.toString().getBytes());
        START.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        RECORD.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        END.headers().add(StreamMarker.HEADER, StreamMarker.END.toString().getBytes());
        END.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());
    }
}
