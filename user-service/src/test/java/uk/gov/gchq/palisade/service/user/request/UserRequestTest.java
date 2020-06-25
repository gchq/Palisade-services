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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class UserRequestTest {

    @Autowired
    private JacksonTester<UserRequest> jsonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseUserRequestToJson() throws IOException {

        Map<String, String> context = new HashMap<>();
        context.put("key1", "context1");
        context.put("key2", "context2");

        UserRequest userRequest = UserRequest.Builder.create()
                .withUser("testUserId")
                .withResource("testResourceId")
                .withContext(context);

        JsonContent<UserRequest> request = jsonTester.write(userRequest);

        //these tests are each for strings
        assertThat(request).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId");
        assertThat(request).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");

        //test is for a json representation of a Map<String, String>, should stay unchanged
        assertThat(request).extractingJsonPathMapValue("$.context").containsKey("key1");
        assertThat(request).extractingJsonPathMapValue("$.context").containsValue("context2");

    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToUserRequest() throws IOException {


        String jsonString = "{\"userId\":\"testUserId\",\"resourceId\":\"testResourceId\",\"context\":{\"key1\":\"context1\",\"key2\":\"context2\"}}";
        ObjectContent userRequest = (ObjectContent) this.jsonTester.parse(jsonString);

        UserRequest request = (UserRequest) userRequest.getObject();
        assertThat(request.userId).isEqualTo("testUserId");
        assertThat(request.getResourceId()).isEqualTo("testResourceId");
    }


}