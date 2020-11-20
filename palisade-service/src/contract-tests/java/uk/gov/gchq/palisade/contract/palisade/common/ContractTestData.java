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

package uk.gov.gchq.palisade.contract.palisade.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

/**
 * Common test data used in the KafkaContractTest
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class ContractTestData {

    public static final JsonNode REQUEST_NODE;
    public static final PalisadeRequest REQUEST_OBJ;
    public static final String REQUEST_JSON = "{\"userId\":\"testUserId\",\"resourceId\":\"/test/resourceId\",\"context\":{\"contents\":{\"purpose\":\"testContext\"}}}";
    public static final String REQUEST_TOKEN = "test-request-token";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, PalisadeRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    private ContractTestData() {
        // hide the constructor, this is just a collection of static objects
    }
}