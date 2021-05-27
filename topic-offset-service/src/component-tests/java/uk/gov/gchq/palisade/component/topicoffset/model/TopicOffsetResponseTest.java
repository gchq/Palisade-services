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
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for evaluating the TopicOffsetResponse, and the related serialising to a JSON string
 * and deserialising back to an object.
 */
class TopicOffsetResponseTest {

    /**
     * Create the object with the constructor and then convert to the Json equivalent.
     * Takes the JSON Object, deserialise and tests against the original Object.
     *
     * @throws JsonProcessingException if the {@link TopicOffsetResponse} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testTopicOffsetResponseSerialisingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var topicOffsetResponse = TopicOffsetResponse.Builder.create().withOffset(101L);

        var actualJson = mapper.writeValueAsString(topicOffsetResponse);
        var actualInstance = mapper.readValue(actualJson, topicOffsetResponse.getClass());

        assertThat(topicOffsetResponse)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(actualInstance);

        assertThat(topicOffsetResponse)
                .as("Check %s using recursion that the serialised and deseralised objects are the same", topicOffsetResponse.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(actualInstance);
    }
}
