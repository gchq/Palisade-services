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

package uk.gov.gchq.palisade.contract.user.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.user.common.Token;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
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

    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();
    public static final JsonNode REQUEST_NODE;
    public static final JsonNode NO_USER_ID_REQUEST_NODE;
    public static final UserRequest REQUEST_OBJ;
    public static final UserRequest NO_USER_ID_REQUEST_OBJ;
    public static final String REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"@type\":\"Context\",\"contents\":{\"purpose\":\"purpose\"}}}";
    public static final String NO_USER_JSON = "{\"userId\":\"invalid-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"@type\":\"Context\",\"contents\":{\"purpose\":\"purpose\"}}}";


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
