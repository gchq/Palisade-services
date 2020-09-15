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
package uk.gov.gchq.palisade.component.filteredresource.message;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.message.TopicOffsetMessage;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {FilteredResourceApplication.class})
class TopicOffsetMessageTest {

    @Autowired
    private JacksonTester<TopicOffsetMessage> jsonTester;

    @Test
    void contextLoads() {
        assertThat(jsonTester).isNotNull();
    }

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link TopicOffsetMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantTopicOffsetMessageSerialisingAndDeserialising() throws IOException {
        TopicOffsetMessage topicOffsetMessage = TopicOffsetMessage.Builder.create()
                .withQueuePointer(42L);

        JsonContent<TopicOffsetMessage> topicOffsetMessageJsonContent = jsonTester.write(topicOffsetMessage);
        ObjectContent<TopicOffsetMessage> topicOffsetMessageObjectContent = jsonTester.parse(topicOffsetMessageJsonContent.getJson());
        TopicOffsetMessage topicOffsetMessageObject = topicOffsetMessageObjectContent.getObject();

        assertAll("TopicOffsetSerialisingDeseralisingAndComparison",
                () -> assertAll("TopicOffsetSerialisingComparedToString",
                        () -> assertThat(topicOffsetMessageJsonContent)
                                .extractingJsonPathNumberValue("$.queuePointer")
                                .isEqualTo(42)
                ),
                () -> assertAll("TopicOffsetDeserialisingComparedToObject",
                        () -> assertThat(topicOffsetMessageObject.queuePointer)
                                .isEqualTo(topicOffsetMessage.queuePointer)
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(topicOffsetMessageObject)
                                .isEqualTo(topicOffsetMessage)
                )
        );
    }
}
