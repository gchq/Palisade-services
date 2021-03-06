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
package uk.gov.gchq.palisade.component.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyResponseTest extends CommonTestData {

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws JsonProcessingException throws if the {@link PolicyResponse} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testPolicyResponseSerialisingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var actualJson = mapper.writeValueAsString(POLICY_RESPONSE);
        var actualInstance = mapper.readValue(actualJson, POLICY_RESPONSE.getClass());

        assertThat(actualInstance)
                .as("The serialised and deserialised object should match the original")
                .isEqualTo(POLICY_RESPONSE);

        assertThat(actualInstance)
                .usingRecursiveComparison()
                .as("The serialised and deserialised object should have the same values as the original")
                .isEqualTo(POLICY_RESPONSE);
    }
}
