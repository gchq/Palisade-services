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
package uk.gov.gchq.palisade.service.user.request;

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
public class UserResponseTest {

    @Autowired
    private JacksonTester<UserResponse> jsonTester;


    /**
     * Tests the creation of the message type, UserResponse using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link UserResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or de-serialise the Json string.
     */
    @Test
    public void testUserResponseSerialisingAndDeserialising() throws IOException {

        User user = new User().userId("testUserId");

        Context context = new Context().purpose("testContext");
        UserResponse userResponse = UserResponse.Builder.create()
                .withUserId("originalUserId")
                .withResourceId("testResourceId")
                .withContext(context)
                .withUser(user);

        JsonContent<UserResponse> userResponseJsonContent = jsonTester.write(userResponse);
        ObjectContent<UserResponse> userResponseObjectContent = this.jsonTester.parse(userResponseJsonContent.getJson());
        UserResponse userResponseObject = userResponseObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserId"),
                        () -> assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId")
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(userResponseObject.getUserId()).isEqualTo(userResponse.getUserId()),
                        () -> assertThat(userResponseObject.getResourceId()).isEqualTo(userResponse.getResourceId()),
                        () -> assertThat(userResponseObject.getContext()).isEqualTo(userResponse.getContext()),
                        () -> assertThat(userResponseObject.user).isEqualTo(user)
                ),

                () -> assertAll("ObjectComparison",
                        () -> assertThat(userResponseObject).isEqualTo(userResponse)
                )
        );
    }
}