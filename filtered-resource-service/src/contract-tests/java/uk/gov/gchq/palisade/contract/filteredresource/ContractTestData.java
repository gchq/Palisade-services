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

package uk.gov.gchq.palisade.contract.filteredresource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;

/**
 * Strictly control content of messages by specifying exact JSON string contents, then converting to appropriate object types.
 * This enforces the contract between services through specifying the string values for messages, and any changes to the contract
 * require changes to the string values.
 */
public final class ContractTestData {

    private ContractTestData() {
    }

    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    private static final String TOPIC_OFFSET_MESSAGE_JSON = "{\"commitOffset\":1}";
    public static final String REQUEST_TOKEN = "test-request-token";

    public static final JsonNode TOPIC_OFFSET_MSG_JSON_NODE;
    public static final TopicOffsetMessage TOPIC_OFFSET_MESSAGE;

    static {
        try {
            TOPIC_OFFSET_MSG_JSON_NODE = MAPPER.readTree(TOPIC_OFFSET_MESSAGE_JSON);
            TOPIC_OFFSET_MESSAGE = ContractTestData.MAPPER.treeToValue(ContractTestData.TOPIC_OFFSET_MSG_JSON_NODE, TopicOffsetMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to setup contract test data", e);
        }
    }
}
