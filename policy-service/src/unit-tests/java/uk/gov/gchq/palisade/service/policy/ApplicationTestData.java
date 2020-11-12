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

package uk.gov.gchq.palisade.service.policy;

import org.apache.kafka.clients.producer.ProducerRecord;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.contract.policy.common.StreamMarker;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;
import uk.gov.gchq.palisade.service.policy.model.Token;

import java.io.Serializable;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ApplicationTestData {
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
    public static final Rules<Serializable> RULES = new Rules<Serializable>().addRule(RULE_MESSAGE, new PassThroughRule<Serializable>());
    public static final PolicyRequest REQUEST = PolicyRequest.Builder.create()
            .withUserId(USER_ID.getId())
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withUser(USER)
            .withResource(LEAF_RESOURCE);
    public static final PolicyResponse RESPONSE = PolicyResponse.Builder.create(REQUEST)
            .withResource(LEAF_RESOURCE)
            .withRules(RULES);
    public static final ProducerRecord<String, PolicyRequest> START = new ProducerRecord<>("resource", 0, null, null);
    public static final ProducerRecord<String, PolicyRequest> RECORD = new ProducerRecord<>("resource", 0, null, REQUEST);
    public static final ProducerRecord<String, PolicyRequest> END = new ProducerRecord<>("resource", 0, null, null);
    // Create a stream of resources, uniquely identifiable by their type, which is their position in the stream (first resource has type "0", second has type "1", etc.)
    public static final Supplier<Stream<ProducerRecord<String, PolicyRequest>>> RECORD_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> PolicyRequest.Builder.create()
                    .withUserId(USER_ID.getId())
                    .withResourceId(RESOURCE_ID)
                    .withContext(CONTEXT)
                    .withUser(USER)
                    .withResource(LEAF_RESOURCE))
            .map(request -> new ProducerRecord<>(
                    "resource",
                    0,
                    (String) null,
                    request,
                    RECORD.headers()));

    static {
        START.headers().add(StreamMarker.HEADER, StreamMarker.START.toString().getBytes());
        START.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        RECORD.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());

        END.headers().add(StreamMarker.HEADER, StreamMarker.END.toString().getBytes());
        END.headers().add(Token.HEADER, REQUEST_TOKEN.getBytes());
    }

    /**
     * Common test data for all classes
     */

    private ApplicationTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }
}
