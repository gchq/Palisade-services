/*
 * Copyright 2018-2021 Crown Copyright
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

package uk.gov.gchq.palisade.component.palisade.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeSystemResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = PalisadeClientRequestTest.class)
class PalisadeSystemResponseTest extends CommonTestData {

    @Autowired
    private JacksonTester<PalisadeSystemResponse> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original response Object.
     *
     * @throws IOException throws if the {@link PalisadeSystemResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testPalisadeSystemResponseSerialisingAndDeserialising() throws IOException {

        JsonContent<PalisadeSystemResponse> requestJsonContent = jsonTester.write(SYSTEM_RESPONSE);
        ObjectContent<PalisadeSystemResponse> requestObjectContent = jsonTester.parse(requestJsonContent.getJson());
        PalisadeSystemResponse requestObject = requestObjectContent.getObject();

        assertAll("PalisadeSystemResponse with request Serialising and Deseralising Comparison",
                () -> assertAll("PalisadeSystemResponse, with PalisadeClientRequest, Serialising Compared To String",
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("/test/resourceId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext")
                ),
                () -> assertAll("PalisadeSystemResponse, with PalisadeClientRequest, Deserialising Compared To Object",
                        () -> assertThat(requestObject.getUserId()).isEqualTo(SYSTEM_RESPONSE.getUserId()),
                        () -> assertThat(requestObject.getResourceId()).isEqualTo(SYSTEM_RESPONSE.getResourceId()),
                        () -> assertThat(requestObject.getContext().getPurpose()).isEqualTo(SYSTEM_RESPONSE.getContext().getPurpose())
                ),
                () -> assertAll("Object Comparison",
                        //compares the two objects using the objects equal method
                        () -> assertThat(requestObject).usingRecursiveComparison().isEqualTo(SYSTEM_RESPONSE)
                )
        );
    }
}
