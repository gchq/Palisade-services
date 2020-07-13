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

@RunWith(SpringRunner.class)
@JsonTest
public class ResourceRequestTest {


    @Autowired
    private JacksonTester<ResourceRequest> jacksonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceRequestToJson() throws IOException {

        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        ResourceRequest resourceRequest = ResourceRequest.Builder.create()
                .withUserId("originalUserId")
                .withResourceId("testResourceId")
                .withContext(context)
                .withUser(user);

        JsonContent<ResourceRequest> resourceRequestJsonContent = jacksonTester.write(resourceRequest);

        //these tests are each for strings
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserId");
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId");

    }

    /**
     * Create the ResourceRequest object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceRequest() throws IOException {

        String jsonString = "{\"userId\":\"originalUserId\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"user\":{\"userId\":{\"id\":\"testUserId\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}";

        ObjectContent<ResourceRequest> resourceRequestContent = jacksonTester.parse(jsonString);

        ResourceRequest request = resourceRequestContent.getObject();
        assertThat(request.getUserId()).isEqualTo("originalUserId");
        assertThat(request.resourceId).isEqualTo("testResourceId");
        assertThat(request.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(request.getUser().getUserId().getId()).isEqualTo("testUserId");
    }


}