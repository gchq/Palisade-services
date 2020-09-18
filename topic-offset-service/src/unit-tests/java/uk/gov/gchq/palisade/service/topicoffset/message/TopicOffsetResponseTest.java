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
package uk.gov.gchq.palisade.service.topicoffset.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the evaluating the TopicOffsetResponse and the related seralising to a JSon string
 * and deseralising back to an object.
 */
@JsonTest
class TopicOffsetResponseTest {

    @Autowired
    private JacksonTester<TopicOffsetResponse> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the constructor and then convert to the Json equivalent.
     * Takes the JSON Object, deserialise and tests against the original Object
     *
     * @throws IOException throws if the {@link TopicOffsetResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void testGroupedDependantQueryScopeResponseSerialisingAndDeserialising() throws IOException {

        TopicOffsetResponse originalTopicOffsetResponse = TopicOffsetResponse.Builder.create().withOffset(Long.valueOf(101));

        JsonContent<TopicOffsetResponse> topicOffsetResponseJsonContent = jsonTester.write(originalTopicOffsetResponse);
        ObjectContent<TopicOffsetResponse> offsetResponseObjectContent = jsonTester.parse(topicOffsetResponseJsonContent.getJson());
        TopicOffsetResponse topicOffsetResponse = offsetResponseObjectContent.getObject();

        assertAll("SerialisingDeseralisingAndComparison",
                () -> assertThat(topicOffsetResponseJsonContent).extractingJsonPathNumberValue("$.commitOffset").isEqualTo(101),
                () -> assertThat(topicOffsetResponse.getCommitOffset()).isEqualTo(originalTopicOffsetResponse.getCommitOffset()),
                () -> assertThat(topicOffsetResponse).isEqualTo(originalTopicOffsetResponse));
    }
}