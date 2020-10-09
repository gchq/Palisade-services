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
package uk.gov.gchq.palisade.service.resource.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class ResourceRequestTest {

    @Autowired
    private JacksonTester<ResourceRequest> jsonTester;

    /**
     * Tests the creation of the message type, ResourceRequest using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link ResourceRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or de-serialise the string.
     */
    @Test
    public void testResourceRequestSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        ResourceRequest resourceRequest = ResourceRequest.Builder.create()
                .withUserId("originalUserId")
                .withResourceId("testResourceId")
                .withContext(context)
                .withUser(user);

        JsonContent<ResourceRequest> resourceRequestJsonContent = jsonTester.write(resourceRequest);
        ObjectContent<ResourceRequest> resourceRequestObjectContent = jsonTester.parse(resourceRequestJsonContent.getJson());
        ResourceRequest resourceRequestObject = resourceRequestObjectContent.getObject();


        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserId"),
                        () -> assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(resourceRequest.getUserId()).isEqualTo(resourceRequestObject.getUserId()),
                        () -> assertThat(resourceRequest.getResourceId()).isEqualTo(resourceRequestObject.getResourceId()),
                        () -> assertThat(resourceRequest.getContext()).isEqualTo(resourceRequestObject.getContext()),
                        () -> assertThat(resourceRequest.getUser()).isEqualTo((resourceRequestObject.getUser()))
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(resourceRequest).isEqualTo(resourceRequestObject)
                )
        );

    }

}
