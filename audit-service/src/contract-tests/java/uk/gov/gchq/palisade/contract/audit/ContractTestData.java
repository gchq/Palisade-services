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

import uk.gov.gchq.palisade.service.audit.common.Token;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

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
    public static final JsonNode ERROR_REQUEST_NODE;
    public static final JsonNode GOOD_SUCCESS_REQUEST_NODE;
    public static final JsonNode BAD_SUCCESS_REQUEST_NODE;
    public static final JsonNode BAD_REQUEST_NODE;
    public static final AuditErrorMessage ERROR_REQUEST_OBJ;
    public static final AuditSuccessMessage GOOD_SUCCESS_REQUEST_OBJ;
    public static final AuditSuccessMessage BAD_SUCCESS_REQUEST_OBJ;
    public static final BadRequest BAD_REQUEST_OBJ;
    public static final String ERROR_REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.service.audit.common.Context\",\"contents\":{\"purpose\":\"purpose\"}},\"serviceName\":\"user-service\",\"timestamp\":\"2020-01-01\",\"serverIP\":\"The IP of the server\",\"serverHostname\":\"The name of the server\",\"attributes\":{\"messages\":\"5\"},\"error\":{\"cause\":null,\"stackTrace\":[],\"message\":\"exception message\",\"suppressed\":[],\"localizedMessage\":\"exception message\"}}";
    public static final String GOOD_SUCCESS_REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.service.audit.common.Context\",\"contents\":{\"purpose\":\"purpose\"}},\"serviceName\":\"data-service\",\"timestamp\":\"2020-01-01\",\"serverIP\":\"The IP of the server\",\"serverHostname\":\"The name of the server\",\"attributes\":{\"messages\":\"5\"},\"leafResourceId\":\"file:/test/resource/file.txt\"}";
    public static final String BAD_SUCCESS_REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.service.audit.common.Context\",\"contents\":{\"purpose\":\"purpose\"}},\"serviceName\":\"user-service\",\"timestamp\":\"2020-01-01\",\"serverIP\":\"The IP of the server\",\"serverHostname\":\"The name of the server\",\"attributes\":{\"messages\":\"5\"},\"leafResourceId\":\"file:/test/resource/file.txt\"}";
    public static final String BAD_REQUEST_JSON = "{\"message\":\"This is a message that should not be deserialised by the Audit-Service\"}";

    static {
        try {
            ERROR_REQUEST_NODE = MAPPER.readTree(ERROR_REQUEST_JSON);
            GOOD_SUCCESS_REQUEST_NODE = MAPPER.readTree(GOOD_SUCCESS_REQUEST_JSON);
            BAD_SUCCESS_REQUEST_NODE = MAPPER.readTree(BAD_SUCCESS_REQUEST_JSON);
            BAD_REQUEST_NODE = MAPPER.readTree(BAD_REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            ERROR_REQUEST_OBJ = MAPPER.treeToValue(ERROR_REQUEST_NODE, AuditErrorMessage.class);
            GOOD_SUCCESS_REQUEST_OBJ = MAPPER.treeToValue(GOOD_SUCCESS_REQUEST_NODE, AuditSuccessMessage.class);
            BAD_SUCCESS_REQUEST_OBJ = MAPPER.treeToValue(BAD_SUCCESS_REQUEST_NODE, AuditSuccessMessage.class);
            BAD_REQUEST_OBJ = MAPPER.treeToValue(BAD_REQUEST_NODE, BadRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> ERROR_FACTORY_JSON = i -> ERROR_REQUEST_JSON;
    public static final Function<Integer, String> GOOD_SUCCESS_FACTORY_JSON = i -> GOOD_SUCCESS_REQUEST_JSON;
    public static final Function<Integer, String> BAD_SUCCESS_FACTORY_JSON = i -> BAD_SUCCESS_REQUEST_JSON;
    public static final Function<Integer, String> BAD_FACTORY_JSON = i -> BAD_REQUEST_JSON;
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
    public static final Function<Integer, JsonNode> BAD_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(BAD_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse error contract test data", e);
        }
    };

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final Headers REQUEST_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes())});


    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> GOOD_SUCCESS_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("success", 0, null, GOOD_SUCCESS_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> BAD_SUCCESS_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("success", 0, null, BAD_SUCCESS_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> BAD_SUCCESS_MESSAGE_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("success", 0, null, BAD_FACTORY_NODE.apply(i), REQUEST_HEADERS));

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> BAD_ERROR_MESSAGE_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("error", 0, null, BAD_FACTORY_NODE.apply(i), REQUEST_HEADERS));
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> ERROR_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("error", 0, null, ERROR_FACTORY_NODE.apply(i), REQUEST_HEADERS));


}
