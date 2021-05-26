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
package uk.gov.gchq.palisade.component.topicoffset.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the evaluating the TopicOffsetRequest, and the related serialising to a JSon string
 * and deserialising back to an object.
 */
class TopicOffsetRequestTest {

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original object.
     *
     * @throws JsonProcessingException throws if the {@link TopicOffsetRequest} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testTopicOffsetRequestSerialisingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var topicOffsetRequest = TopicOffsetRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(new Context().purpose("testContext"))
                .withResourceNode(NullNode.getInstance());

        var actualJson = mapper.writeValueAsString(topicOffsetRequest);
        var actualInstance = mapper.readValue(actualJson, topicOffsetRequest.getClass());

        assertThat(topicOffsetRequest)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(actualInstance);

        assertThat(topicOffsetRequest)
                .as("Check %s using recursion that the serialised and deseralised objects are the same", topicOffsetRequest.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(actualInstance);
    }
}


