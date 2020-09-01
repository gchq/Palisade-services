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
package uk.gov.gchq.palisade.service.topicoffset.request;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
class TopicOffsetRequestTest {

    @Autowired
    private JacksonTester<TopicOffsetRequest> jsonTester;


    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link TopicOffsetRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void testGroupedDependantQueryScopeResponseSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        TopicOffsetRequest originalTopicOffsetRequest = TopicOffsetRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withResource(resource);

        JsonContent<TopicOffsetRequest> topicOffsetRequestJsonContent = jsonTester.write(originalTopicOffsetRequest);
        ObjectContent<TopicOffsetRequest> topicOffsetRequestObjectContent = jsonTester.parse(topicOffsetRequestJsonContent.getJson());
        TopicOffsetRequest topicOffsetRequest = topicOffsetRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(topicOffsetRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(topicOffsetRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(topicOffsetRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(topicOffsetRequestJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(topicOffsetRequest.getUserId()).isEqualTo(originalTopicOffsetRequest.getUserId()),
                        () -> assertThat(topicOffsetRequest.getContext()).isEqualTo(originalTopicOffsetRequest.getContext()),
                        () -> assertThat(topicOffsetRequest.getResource()).isEqualTo(originalTopicOffsetRequest.getResource()),
                        () -> assertThat(topicOffsetRequest.getResourceId()).isEqualTo(originalTopicOffsetRequest.getResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(topicOffsetRequest.equals(originalTopicOffsetRequest))
                )
        );
    }
}


