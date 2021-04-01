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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the evaluating the TopicOffsetResponse and the related seralising to a JSON string
 * and deseralising back to an object.
 */
@JsonTest
@ContextConfiguration(classes = {TopicOffsetResponseTest.class})
class TopicOffsetResponseTest {

    @Autowired
    private JacksonTester<TopicOffsetResponse> jsonTester;

    @Test
    void testContextLoads() {
        assertThat(jsonTester).isNotNull();
    }

    /**
     * Create the object with the constructor and then convert to the Json equivalent.
     * Takes the JSON Object, deserialise and tests against the original Object
     *
     * @throws IOException throws if the {@link TopicOffsetResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testTopicOffsetResponseSerialisingAndDeserialising() throws IOException {
        var topicOffsetResponse = TopicOffsetResponse.Builder.create().withOffset(101L);

        var topicOffsetResponseJsonContent = jsonTester.write(topicOffsetResponse);
        var offsetResponseObjectContent = jsonTester.parse(topicOffsetResponseJsonContent.getJson());
        var topicOffsetResponseObject = offsetResponseObjectContent.getObject();

        assertThat(topicOffsetResponse)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(topicOffsetResponseObject);

        assertThat(topicOffsetResponse)
                .as("Check %s using recursion that the serialised and deseralised objects are the same", topicOffsetResponse.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(topicOffsetResponseObject);

    }
}