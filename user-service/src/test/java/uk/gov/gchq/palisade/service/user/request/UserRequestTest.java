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

import java.io.IOException;

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

        Context context = new Context().purpose("testContext");
        UserRequest userRequest = UserRequest.Builder.create()
                .withUserId("testUserId")
                .withResourceId("testResourceId")
                .withContext(context);

        JsonContent<UserRequest> userRequestJsonContent = jsonTester.write(userRequest);

        //these tests are each for strings
        assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId");
        assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(userRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToUserRequest() throws IOException {
        String jsonString = "{\"userId\":\"testUserId\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"" +
                "uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}}}";
        ObjectContent<UserRequest> userRequest = this.jsonTester.parse(jsonString);

        UserRequest request = userRequest.getObject();
        assertThat(request.userId).isEqualTo("testUserId");
        assertThat(request.getResourceId()).isEqualTo("testResourceId");
        assertThat(request.getContext().getPurpose()).isEqualTo("testContext");
    }
}