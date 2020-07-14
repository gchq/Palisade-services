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

@RunWith(SpringRunner.class)
@JsonTest
public class UserResponseTest {


    @Autowired
    private JacksonTester<UserResponse> jsonTester;


    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the UserResponse object cannot be converted to a JsonContent.
     * This equates to a failure to serialise the string.
     */
    @Test
   public void testSerialiseUserResponseToJson() throws IOException {

        Context context = new Context().purpose("testContext");

        User user = new User().userId("testUserId");

        UserResponse userResponse = UserResponse.Builder.create()
                .withUserId("originalUserID")
                .withResource("testResourceId")
                .withContext(context)
                .withUser(user);

        JsonContent<UserResponse> response = jsonTester.write(userResponse);

        //these tests are each for strings
        assertThat(response).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(response).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(response).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(response).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId");




    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToUserResponse() throws IOException {

        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"user\":{\"userId\":{\"id\":\"testUserId\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}";
        ObjectContent userResponseContent = (ObjectContent) this.jsonTester.parse(jsonString);
        UserResponse response = (UserResponse) userResponseContent.getObject();

        assertThat(response.getUserId()).isEqualTo("originalUserID");
        assertThat(response.user.getUserId().getId()).isEqualTo("testUserId");
        assertThat(response.getResourceId()).isEqualTo("testResourceId");

    }

    /**
     * Create the UserResponse object from a UserRequest, serialise it and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testSerialiseUserResponseUsingUserRequestToJson() throws IOException {

        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");

        UserRequest userRequest = UserRequest.Builder.create()

                .withUser("originalUserID")
                .withResource("originalResourceID")
                .withContext(context);

        UserResponse policyResponse = UserResponse.Builder.create(userRequest).withUser(user);

        JsonContent<UserResponse> userResponseJsonContent = jsonTester.write(policyResponse);

        assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID");
        assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(userResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId");

    }
}