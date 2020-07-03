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
package uk.gov.gchq.palisade.service.user.response;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
class UserResponseTest {


    @Autowired
    private JacksonTester<UserResponse> jsonTester;


    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the UserResponse object cannot be converted to a JsonContent.
     * This equates to a failure to serialise the string.
     */
    @Test
    void testSerialiseUserResponseToJson() throws IOException {

        Map<String, String> context = context = new HashMap<>();
        context.put("key1", "context1");
        context.put("key2", "context2");

        User user = User.create("testUserId");

        UserResponse userResponse = UserResponse.Builder.create()
                .withResource("testResourceId")
                .withContext(context)
                .withUser(user);

        JsonContent<UserResponse> response = jsonTester.write(userResponse);

        //these tests are each for strings
        assertThat(response).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");

        //test is for a json representation of a Map<String, String>, should stay unchanged
        assertThat(response).extractingJsonPathMapValue("$.context").containsKey("key1");
        assertThat(response).extractingJsonPathMapValue("$.context").containsValue("context2");


    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToUserResponse() throws IOException {

        String jsonString = "{\"resourceId\":\"testResourceId\",\"context\":{\"key1\":\"context1\",\"key2\":\"context2\"},\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}}}";
        ObjectContent userResponseContent = (ObjectContent) this.jsonTester.parse(jsonString);
        UserResponse response = (UserResponse) userResponseContent.getObject();
        assertThat(response.user.userId).isEqualTo("testUserId");
        assertThat(response.getResourceId()).isEqualTo("testResourceId");

    }


}