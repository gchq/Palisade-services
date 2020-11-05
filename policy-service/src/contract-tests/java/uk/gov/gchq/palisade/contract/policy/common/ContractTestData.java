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

package uk.gov.gchq.palisade.contract.policy.common;

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

    public static final JsonNode REQUEST_NODE;
    public static final JsonNode NO_RESOURCE_RULES_REQUEST_NODE;
    public static final PolicyRequest REQUEST_OBJ;
    public static final PolicyRequest NO_RESOURCE_RULES_REQUEST_OBJ;
    public static final String REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"file:/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"test-user-id\"},\"roles\":[\"role\"],\"auths\":[\"auth\"],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"file:/test/resourceId\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"file:/test/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"file:/\"}},\"serialisedFormat\":\"txt\",\"type\":\"test\"}}";
    public static final String NO_RESOURCE_RULES_REQUEST_JSON = "{\"userId\":\"noResourceRulesUser\",\"resourceId\":\"file:/test/noRulesResource\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"noResourceRulesUser\"},\"roles\":[\"role\"],\"auths\":[\"auth\"],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"file:/test/noRulesResource\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"file:/test/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"file:/\"}},\"serialisedFormat\":\"txt\",\"type\":\"test\"}}";


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
