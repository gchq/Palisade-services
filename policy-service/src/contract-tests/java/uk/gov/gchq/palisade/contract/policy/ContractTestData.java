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

package uk.gov.gchq.palisade.contract.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.StreamMarker;
import uk.gov.gchq.palisade.service.policy.model.Token;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Common test data for all classes
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class ContractTestData {

    private ContractTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }

    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);
    public static final User USER = new User().userId(USER_ID).roles("role").auths("auth");
    public static final ConnectionDetail CONNECTION_DETAIL = new SimpleConnectionDetail().serviceName("data-service");
    public static final LeafResource RESOURCE = (LeafResource) ResourceBuilder.create(RESOURCE_ID);
    public static final PolicyRequest POLICY_REQUEST;
    public static final JsonNode REQUEST_NODE;
    public static final PolicyRequest REQUEST_OBJ;
    public static String requestJson;

    static {
        RESOURCE.connectionDetail(CONNECTION_DETAIL).serialisedFormat("txt").setType("test");
        POLICY_REQUEST = PolicyRequest.Builder.create()
                .withUserId(USER_ID.getId())
                .withResourceId(RESOURCE.getId())
                .withContext(CONTEXT)
                .withUser(USER)
                .withResource(RESOURCE);
    }

    static {
        try {
            requestJson = MAPPER.writeValueAsString(POLICY_REQUEST);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse PolicyRequest test data", e);
        }
    }

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(requestJson);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, PolicyRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> String.format(requestJson, i, i);
    public static final Function<Integer, JsonNode> REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, PolicyRequest> REQUEST_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(REQUEST_FACTORY_NODE.apply(i), PolicyRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };

    public static final String REQUEST_TOKEN = "test-request-token";

    public static final Headers START_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.START.toString().getBytes())});
    public static final Headers REQUEST_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes())});
    public static final Headers END_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.END.toString().getBytes())});

    public static final ProducerRecord<String, JsonNode> START_RECORD = new ProducerRecord<String, JsonNode>("resource", 0, null, null, START_HEADERS);
    public static final ProducerRecord<String, JsonNode> END_RECORD = new ProducerRecord<String, JsonNode>("resource", 0, null, null, END_HEADERS);

    // Create a stream of resources, uniquely identifiable by their type, which is their position in the stream (first resource has type "0", second has type "1", etc.)
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));
}
