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
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = PalisadeClientRequestTest.class)
class PalisadeClientRequestTest extends CommonTestData {

    @Autowired
    private JacksonTester<PalisadeClientRequest> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original response Object.
     *
     * @throws IOException throws if the {@link PalisadeClientRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testPalisadeRequestSerializingAndDeserializing() throws IOException {

        JsonContent<PalisadeClientRequest> requestJsonContent = jsonTester.write(PALISADE_REQUEST);

        ObjectContent<PalisadeClientRequest> requestObjectContent = jsonTester.parse(requestJsonContent.getJson());
        PalisadeClientRequest palisadeClientRequestObject = requestObjectContent.getObject();

        assertThat(palisadeClientRequestObject).usingRecursiveComparison()
                .as("Recursively compare the PalisadeClientRequest object")
                .isEqualTo(PALISADE_REQUEST);
    }
}
