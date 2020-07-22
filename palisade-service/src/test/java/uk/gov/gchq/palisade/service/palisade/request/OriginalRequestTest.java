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
package uk.gov.gchq.palisade.service.palisade.request;

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
public class OriginalRequestTest {

    @Autowired
    private JacksonTester<OriginalRequest> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the object can not be converted to a Json string.
     */
    @Test
    public void testSerialiseOriginalRequestToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        OriginalRequest originalRequest = OriginalRequest.Builder.create()
                .withUserId("testUser")
                .withResourceId("testResource")
                .withContext(context);
        JsonContent<OriginalRequest> originalRequestJsonContent = jsonTester.write(originalRequest);

        assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUser");
        assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResource");
        assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToOriginalRequest() throws IOException {
        String jsonString = "{\"userId\":\"testUser\",\"resourceId\":\"testResource\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}}}";

        ObjectContent<OriginalRequest> originalRequestObjectContent = this.jsonTester.parse(jsonString);
        OriginalRequest request = originalRequestObjectContent.getObject();
        assertThat(request.getUserId()).isEqualTo("testUser");
        assertThat(request.getResourceId()).isEqualTo("testResource");
    }
}