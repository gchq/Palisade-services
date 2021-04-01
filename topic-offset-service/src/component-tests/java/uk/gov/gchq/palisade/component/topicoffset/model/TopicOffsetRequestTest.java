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

import uk.gov.gchq.palisade.service.topicoffset.common.Context;
import uk.gov.gchq.palisade.service.topicoffset.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.topicoffset.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.topicoffset.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.topicoffset.common.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.topicoffset.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the evaluating the TopicOffsetRequest and the related serialising to a JSon string
 * and deseralising back to an object.
 */
@JsonTest
@ContextConfiguration(classes = {TopicOffsetRequestTest.class})
class TopicOffsetRequestTest {

    @Autowired
    private JacksonTester<TopicOffsetRequest> jsonTester;

    /**
     * Test context loads.
     */
    @Test
    void testContextLoads() {
        assertThat(jsonTester).isNotNull();
    }

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testTopicOffsetRequestSerialisingAndDeserialising() throws IOException {
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        var topicOffsetRequest = TopicOffsetRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(new Context().purpose("testContext"))
                .withResource(resource);

        var topicOffsetRequestJsonContent = jsonTester.write(topicOffsetRequest);
        var topicOffsetRequestObjectContent = jsonTester.parse(topicOffsetRequestJsonContent.getJson());
        var topicOffsetRequestObject = topicOffsetRequestObjectContent.getObject();

        assertThat(topicOffsetRequest)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(topicOffsetRequestObject);

        assertThat(topicOffsetRequest)
                .as("Check %s using recursion that the serialised and deseralised objects are the same", topicOffsetRequest.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(topicOffsetRequestObject);
    }
}


