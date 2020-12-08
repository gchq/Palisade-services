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

import uk.gov.gchq.palisade.service.palisade.model.PalisadeResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = PalisadeResponseTest.class)
class PalisadeResponseTest {

    @Autowired
    private JacksonTester<PalisadeResponse> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, de-serialises and tests against the original response Object.
     *
     * @throws IOException throws if the {@link PalisadeResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to seralise or deseralise the string.
     */
    @Test
    void testPalisadeResponseSerialisingAndDeserialising() throws IOException {
        PalisadeResponse palisadeResponse = new PalisadeResponse("tokenID");

        JsonContent<PalisadeResponse> responseJsonContent = jsonTester.write(palisadeResponse);

        ObjectContent<PalisadeResponse> responseObjectContent = jsonTester.parse(responseJsonContent.getJson());
        PalisadeResponse palisadeResponseObject = responseObjectContent.getObject();

        assertAll("Palisade Response Serialising and Deseralising Comparison",
                () -> assertAll("Palisade Response Serialising Compared To String",
                        () -> assertThat(responseJsonContent).extractingJsonPathStringValue("$.token").isEqualTo("tokenID")
                ),
                () -> assertAll("Palisade Response Deserialising Compared To Object",
                        () -> assertThat(palisadeResponseObject.getToken()).isEqualTo(palisadeResponse.getToken())
                ),
                () -> assertAll("Object Comparison",
                        //compares the two objects using the objects equal method
                        () -> assertThat(palisadeResponseObject).isEqualTo(palisadeResponse)
                )
        );
    }
}
