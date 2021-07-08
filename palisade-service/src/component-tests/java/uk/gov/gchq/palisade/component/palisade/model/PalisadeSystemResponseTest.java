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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeSystemResponse;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = PalisadeClientRequestTest.class)
class PalisadeSystemResponseTest extends CommonTestData {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testContextLoads() {
        assertThat(mapper)
                .as("Check that the ObjectMapper has been autowired successfully")
                .isNotNull();
    }

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original response Object.
     *
     * @throws JsonProcessingException throws if the {@link PalisadeSystemResponse} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testPalisadeSystemResponseSerialisationAndDeserialisation() throws JsonProcessingException {
        var actualJson = mapper.writeValueAsString(SYSTEM_RESPONSE);
        var actualInstance = mapper.readValue(actualJson, SYSTEM_RESPONSE.getClass());

        assertThat(actualInstance)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(SYSTEM_RESPONSE);

        assertThat(actualInstance)
                .as("Recursively check that the PalisadeSystemResponse object has not been modified during serialisation")
                .usingRecursiveComparison()
                .isEqualTo(SYSTEM_RESPONSE);
    }
}
