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
    public static final UserId NO_RESOURCE_RULES_USER_ID = new UserId().id("noResourceRulesUser");
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String NO_RESOURCE_RULES_RESOURCE_ID = "/test/noRulesResource";
    public static final String PURPOSE = "test-purpose";
    public static final String NO_RESOURCE_RULES_PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);
    public static final Context NO_RESOURCE_RULES_CONTEXT = new Context().purpose(NO_RESOURCE_RULES_PURPOSE);
    public static final User USER = new User().userId(USER_ID).roles("role").auths("auth");
    public static final User NO_RESOURCE_RULES_USER = new User().userId(NO_RESOURCE_RULES_USER_ID).roles("role").auths("auth");
    public static final ConnectionDetail CONNECTION_DETAIL = new SimpleConnectionDetail().serviceName("test-data-service");
    public static final LeafResource RESOURCE = (LeafResource) ResourceBuilder.create(RESOURCE_ID);
    public static final LeafResource NO_RESOURCE_RULES_RESOURCE = (LeafResource) ResourceBuilder.create(NO_RESOURCE_RULES_RESOURCE_ID);
    public static final PolicyRequest POLICY_REQUEST;
    public static final PolicyRequest NO_RESOURCE_RULES_POLICY_REQUEST;
    public static final JsonNode REQUEST_NODE;
    public static final JsonNode NO_RESOURCE_RULES_REQUEST_NODE;
    public static final PolicyRequest REQUEST_OBJ;
    public static final PolicyRequest NO_RESOURCE_RULES_REQUEST_OBJ;
    public static final String REQUEST_JSON;
    public static final String NO_RESOURCE_RULES_REQUEST_JSON;

    static {
        RESOURCE.connectionDetail(CONNECTION_DETAIL).serialisedFormat("txt").setType("test");
        POLICY_REQUEST = PolicyRequest.Builder.create()
                .withUserId(USER_ID.getId())
                .withResourceId(RESOURCE.getId())
                .withContext(CONTEXT)
                .withUser(USER)
                .withResource(RESOURCE);

        NO_RESOURCE_RULES_RESOURCE.connectionDetail(CONNECTION_DETAIL).serialisedFormat("txt").setType("test");
        NO_RESOURCE_RULES_POLICY_REQUEST = PolicyRequest.Builder.create()
                .withUserId(NO_RESOURCE_RULES_USER_ID.getId())
                .withResourceId(NO_RESOURCE_RULES_RESOURCE.getId())
                .withContext(NO_RESOURCE_RULES_CONTEXT)
                .withUser(NO_RESOURCE_RULES_USER)
                .withResource(NO_RESOURCE_RULES_RESOURCE);
    }

    static {
        try {
            REQUEST_JSON = MAPPER.writeValueAsString(POLICY_REQUEST);
            NO_RESOURCE_RULES_REQUEST_JSON = MAPPER.writeValueAsString(NO_RESOURCE_RULES_POLICY_REQUEST);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse PolicyRequest test data", e);
        }
    }

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
            NO_RESOURCE_RULES_REQUEST_NODE = MAPPER.readTree(NO_RESOURCE_RULES_REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, PolicyRequest.class);
            NO_RESOURCE_RULES_REQUEST_OBJ = MAPPER.treeToValue(NO_RESOURCE_RULES_REQUEST_NODE, PolicyRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> String.format(REQUEST_JSON, i, i);
    public static final Function<Integer, String> NO_RESOURCE_RULES_REQUEST_FACTORY_JSON = i -> String.format(NO_RESOURCE_RULES_REQUEST_JSON, i, i);
    public static final Function<Integer, JsonNode> REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> NO_RESOURCE_RULES_REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(NO_RESOURCE_RULES_REQUEST_FACTORY_JSON.apply(i));
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
    public static final Function<Integer, PolicyRequest> NO_RESOURCE_RULES_REQUEST_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(NO_RESOURCE_RULES_REQUEST_FACTORY_NODE.apply(i), PolicyRequest.class);
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

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> NO_RESOURCE_RULES_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, NO_RESOURCE_RULES_REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));
}