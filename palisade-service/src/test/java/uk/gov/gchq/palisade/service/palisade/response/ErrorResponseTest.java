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
package uk.gov.gchq.palisade.service.palisade.response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class ErrorResponseTest {

    @Autowired
    private JacksonTester<ErrorResponse> jacksonTester;


    /**
     * Create the ErrorResponse object using the builder and then serialise it to a Json string.
     * Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseErrorResponseToJson() throws IOException {

        ErrorResponse errorResponse = ErrorResponse.Builder.create()
                .withTechnicalMessage("Technical Error Message")
                .withErrorMessage("Error Message");

        JsonContent<ErrorResponse> errorResponseJsonContent = jacksonTester.write(errorResponse);

        //these tests are each for strings
        assertThat(errorResponseJsonContent).extractingJsonPathStringValue("$.technicalMessage").isEqualTo("Technical Error Message");
        assertThat(errorResponseJsonContent).extractingJsonPathStringValue("$.errorMessage").isEqualTo("Error Message");

    }

    /**
     * Create the ErrorResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToUserRequest() throws IOException {

        String jsonString = "{\"technicalMessage\":\"Technical Error Message\",\"errorMessage\":\"Error Message\"}";

        ObjectContent resourceRequestContent = (ObjectContent) jacksonTester.parse(jsonString);

        ErrorResponse errorResponse = (ErrorResponse) resourceRequestContent.getObject();
        assertThat(errorResponse.errorMessage).isEqualTo("Error Message");
        assertThat(errorResponse.technicalMessage).isEqualTo("Technical Error Message");

    }
}