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

package uk.gov.gchq.palisade.contract.user;

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
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.model.StreamMarker;
import uk.gov.gchq.palisade.service.user.model.Token;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

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
    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final UserId NO_USER_ID = new UserId().id("invalid-user-id");
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final Context CONTEXT = new Context().purpose("purpose");
    public static final UserRequest USER_REQUEST;
    public static final UserRequest NO_USER_ID_REQUEST;
    public static final JsonNode REQUEST_NODE;
    public static final JsonNode NO_USER_ID_REQUEST_NODE;
    public static final UserRequest REQUEST_OBJ;
    public static final UserRequest NO_USER_ID_REQUEST_OBJ;
    public static final String REQUEST_JSON;
    public static final String NO_USER_JSON;

    static {
        USER_REQUEST = UserRequest.Builder.create()
                .withUserId(USER_ID.getId())
                .withResourceId(RESOURCE_ID)
                .withContext(CONTEXT);

        NO_USER_ID_REQUEST = UserRequest.Builder.create()
                .withUserId(NO_USER_ID.getId())
                .withResourceId(RESOURCE_ID)
                .withContext(CONTEXT);
    }

    static {
        try {
            REQUEST_JSON = MAPPER.writeValueAsString(USER_REQUEST);
            NO_USER_JSON = MAPPER.writeValueAsString(NO_USER_ID_REQUEST);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse UserRequest test data", e);
        }
    }

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
            NO_USER_ID_REQUEST_NODE = MAPPER.readTree(NO_USER_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, UserRequest.class);
            NO_USER_ID_REQUEST_OBJ = MAPPER.treeToValue(NO_USER_ID_REQUEST_NODE, UserRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> REQUEST_JSON;
    public static final Function<Integer, String> NO_USER_ID_REQUEST_FACTORY_JSON = i -> NO_USER_JSON;
    public static final Function<Integer, JsonNode> REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> NO_USER_ID_REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(NO_USER_ID_REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse error contract test data", e);
        }
    };
    public static final Function<Integer, UserRequest> REQUEST_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(REQUEST_FACTORY_NODE.apply(i), UserRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };
    public static final Function<Integer, UserRequest> NO_USER_ID_REQUEST_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(NO_USER_ID_REQUEST_FACTORY_NODE.apply(i), UserRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert error contract test data to objects", e);
        }
    };

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final Headers START_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.START.toString().getBytes())});
    public static final Headers REQUEST_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes())});
    public static final Headers END_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.END.toString().getBytes())});
    public static final ProducerRecord<String, JsonNode> START_RECORD = new ProducerRecord<String, JsonNode>("request", 0, null, null, START_HEADERS);
    public static final ProducerRecord<String, JsonNode> END_RECORD = new ProducerRecord<String, JsonNode>("request", 0, null, null, END_HEADERS);

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("request", 0, null, REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> NO_USER_ID_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("request", 0, null, NO_USER_ID_REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));
}