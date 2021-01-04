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
package uk.gov.gchq.palisade.component.palisade.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = PalisadeRequestTest.class)
class PalisadeRequestTest extends CommonTestData {

    @Autowired
    private JacksonTester<PalisadeRequest> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original response Object.
     *
     * @throws IOException throws if the {@link PalisadeRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testPalisadeRequestSerialisingAndDeserialising() throws IOException {

        JsonContent<PalisadeRequest> requestJsonContent = jsonTester.write(PALISADE_REQUEST);

        ObjectContent<PalisadeRequest> requestObjectContent = jsonTester.parse(requestJsonContent.getJson());
        PalisadeRequest palisadeRequestObject = requestObjectContent.getObject();

        assertAll("PalisadeRequest Serialising and Deseralising Comparison",
                () -> assertAll("PalisadeRequest Serialising Compared To String",
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("/test/resourceId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext")
                ),
                () -> assertAll("PalisadeRequest Deserialising Compared To Object",
                        () -> assertThat(palisadeRequestObject.getUserId()).isEqualTo(PALISADE_REQUEST.getUserId()),
                        () -> assertThat(palisadeRequestObject.getResourceId()).isEqualTo(PALISADE_REQUEST.getResourceId()),
                        () -> assertThat(palisadeRequestObject.getContext()).isEqualTo(PALISADE_REQUEST.getContext())
                ),
                () -> assertAll("Object Comparison",
                        //compares the two objects using the objects equal method
                        () -> assertThat(palisadeRequestObject).usingRecursiveComparison().isEqualTo(PALISADE_REQUEST)
                )
        );
    }
}
