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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class ResourceResponseTest {

    @Autowired
    private JacksonTester<ResourceResponse> jsonTester;

    /**
     * Tests the creation of the message type, ResourceResponse using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link ResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or de-serialise the string.
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        ResourceResponse resourceResponse = ResourceResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource);

        JsonContent<ResourceResponse> resourceResponseJsonContent = jsonTester.write(resourceResponse);
        ObjectContent<ResourceResponse> resourceResponseObjectContent = jsonTester.parse(resourceResponseJsonContent.getJson());
        ResourceResponse resourceResponseObject = resourceResponseObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(resourceResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(resourceResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(resourceResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(resourceResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(resourceResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(resourceResponse.getUserId()).isEqualTo(resourceResponseObject.getUserId()),
                        () -> assertThat(resourceResponse.getResourceId()).isEqualTo(resourceResponseObject.getResourceId()),
                        () -> assertThat(resourceResponse.getContext()).isEqualTo(resourceResponseObject.getContext()),
                        () -> assertThat(resourceResponse.getUser()).isEqualTo((resourceResponseObject.getUser())),
                        () -> assertThat(resourceResponse.resource).isEqualTo((resourceResponseObject.resource))
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(resourceResponse).isEqualTo(resourceResponseObject)
                )
        );
    }

}
