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
package uk.gov.gchq.palisade.component.user.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.user.request.UserRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = {UserRequestTest.class})
public class UserRequestTest {

    @Autowired
    private JacksonTester<UserRequest> jsonTester;

    /**
     * Tests the creation of the message type, UserRequest using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link UserRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the Json string.
     */
    @Test
    public void testUserRequestSerialisingAndDeserialising() throws IOException {

        Context context = new Context().purpose("testContext");
        UserRequest userRequest = UserRequest.Builder.create()
                .withUserId("testUserId")
                .withResourceId("testResourceId")
                .withContext(context);

        JsonContent<UserRequest> userRequestJsonContent = jsonTester.write(userRequest);
        ObjectContent<UserRequest> userRequestObjectContent = this.jsonTester.parse(userRequestJsonContent.getJson());
        UserRequest userRequestObject = userRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId"),
                        () -> assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(userRequestObject.userId).isEqualTo(userRequest.getUserId()),
                        () -> assertThat(userRequestObject.getResourceId()).isEqualTo(userRequest.getResourceId()),
                        () -> assertThat(userRequestObject.getContext()).isEqualTo(userRequest.getContext())
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(userRequestObject).isEqualTo(userRequest)
                )
        );
    }


}