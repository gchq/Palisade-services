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

package uk.gov.gchq.palisade.contract.audit;

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
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.model.Token;
import uk.gov.gchq.palisade.service.audit.service.ServiceName;

import java.util.HashMap;
import java.util.Map;
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
    public static final String USER_ID = "test-user-id";
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final Context CONTEXT = new Context().purpose("purpose");
    public static final String TIMESTAMP = "2020-01-01";
    public static final String SERVER_IP = "The IP of the server";
    public static final String SERVER_NAME = "The name of the server";
    public static final Map<String, Object> ATTRIBUTE_MAP = new HashMap<>();
    public static final Throwable EXCEPTION = new Throwable("exception message");
    public static final String LEAF_RESOURCE_ID = "file:/test/resource/file.txt";
    public static final AuditErrorMessage ERROR_REQUEST;
    public static final AuditSuccessMessage GOOD_SUCCESS_REQUEST;
    public static final AuditSuccessMessage BAD_SUCCESS_REQUEST;
    public static final JsonNode ERROR_REQUEST_NODE;
    public static final JsonNode GOOD_SUCCESS_REQUEST_NODE;
    public static final JsonNode BAD_SUCCESS_REQUEST_NODE;
    public static final AuditErrorMessage ERROR_REQUEST_OBJ;
    public static final AuditSuccessMessage GOOD_SUCCESS_REQUEST_OBJ;
    public static final AuditSuccessMessage BAD_SUCCESS_REQUEST_OBJ;
    public static final String ERROR_REQUEST_JSON;
    public static final String GOOD_SUCCESS_REQUEST_JSON;
    public static final String BAD_SUCCESS_REQUEST_JSON;

    static {
        ATTRIBUTE_MAP.put("messages", "5");
    }

    static {
        ERROR_REQUEST = AuditErrorMessage.Builder.create()
                .withUserId(USER_ID)
                .withResourceId(RESOURCE_ID)
                .withContext(CONTEXT)
                .withServiceName(ServiceName.USER_SERVICE.value)
                .withTimestamp(TIMESTAMP)
                .withServerIp(SERVER_IP)
                .withServerHostname(SERVER_NAME)
                .withAttributes(ATTRIBUTE_MAP)
                .withError(EXCEPTION);

        GOOD_SUCCESS_REQUEST = AuditSuccessMessage.Builder.create()
                .withUserId(USER_ID)
                .withResourceId(RESOURCE_ID)
                .withContext(CONTEXT)
                .withServiceName(ServiceName.DATA_SERVICE.value)
                .withTimestamp(TIMESTAMP)
                .withServerIp(SERVER_IP)
                .withServerHostname(SERVER_NAME)
                .withAttributes(ATTRIBUTE_MAP)
                .withLeafResourceId(LEAF_RESOURCE_ID);

        BAD_SUCCESS_REQUEST = AuditSuccessMessage.Builder.create()
                .withUserId(USER_ID)
                .withResourceId(RESOURCE_ID)
                .withContext(CONTEXT)
                .withServiceName(ServiceName.USER_SERVICE.value)
                .withTimestamp(TIMESTAMP)
                .withServerIp(SERVER_IP)
                .withServerHostname(SERVER_NAME)
                .withAttributes(ATTRIBUTE_MAP)
                .withLeafResourceId(LEAF_RESOURCE_ID);
    }

    static {
        try {
            ERROR_REQUEST_JSON = MAPPER.writeValueAsString(ERROR_REQUEST);
            GOOD_SUCCESS_REQUEST_JSON = MAPPER.writeValueAsString(GOOD_SUCCESS_REQUEST);
            BAD_SUCCESS_REQUEST_JSON = MAPPER.writeValueAsString(BAD_SUCCESS_REQUEST);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse UserRequest test data", e);
        }
    }

    static {
        try {
            ERROR_REQUEST_NODE = MAPPER.readTree(ERROR_REQUEST_JSON);
            GOOD_SUCCESS_REQUEST_NODE = MAPPER.readTree(GOOD_SUCCESS_REQUEST_JSON);
            BAD_SUCCESS_REQUEST_NODE = MAPPER.readTree(BAD_SUCCESS_REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            ERROR_REQUEST_OBJ = MAPPER.treeToValue(ERROR_REQUEST_NODE, AuditErrorMessage.class);
            GOOD_SUCCESS_REQUEST_OBJ = MAPPER.treeToValue(GOOD_SUCCESS_REQUEST_NODE, AuditSuccessMessage.class);
            BAD_SUCCESS_REQUEST_OBJ = MAPPER.treeToValue(BAD_SUCCESS_REQUEST_NODE, AuditSuccessMessage.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> ERROR_FACTORY_JSON = i -> ERROR_REQUEST_JSON;
    public static final Function<Integer, String> GOOD_SUCCESS_FACTORY_JSON = i -> GOOD_SUCCESS_REQUEST_JSON;
    public static final Function<Integer, String> BAD_SUCCESS_FACTORY_JSON = i -> BAD_SUCCESS_REQUEST_JSON;
    public static final Function<Integer, JsonNode> ERROR_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(ERROR_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> GOOD_SUCCESS_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(GOOD_SUCCESS_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse error contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> BAD_SUCCESS_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(BAD_SUCCESS_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse error contract test data", e);
        }
    };
    public static final Function<Integer, AuditErrorMessage> ERROR_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(ERROR_FACTORY_NODE.apply(i), AuditErrorMessage.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };
    public static final Function<Integer, AuditSuccessMessage> GOOD_SUCCESS_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(GOOD_SUCCESS_FACTORY_NODE.apply(i), AuditSuccessMessage.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert error contract test data to objects", e);
        }
    };
    public static final Function<Integer, AuditSuccessMessage> BAD_SUCCESS_FACTORY_OBJ = i -> {
        try {
            return MAPPER.treeToValue(BAD_SUCCESS_FACTORY_NODE.apply(i), AuditSuccessMessage.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert error contract test data to objects", e);
        }
    };

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final Headers REQUEST_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes())});

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> ERROR_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("error", 0, null, ERROR_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> GOOD_SUCCESS_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("success", 0, null, GOOD_SUCCESS_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> BAD_SUCCESS_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("success", 0, null, BAD_SUCCESS_FACTORY_NODE.apply(i), REQUEST_HEADERS));
}
