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

package uk.gov.gchq.palisade.service.policy.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;
import uk.gov.gchq.palisade.service.request.Policy;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;

public class SetTypePolicyRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final User testUser = new User().userId("TestUser");
    private Policy typePolicy;

    @BeforeEach
    public void setup() {
        typePolicy = new Policy().owner(testUser).resourceLevelRule("Testing purpose", new IsTextResourceRule());
    }

    @Test
    public void testSetTypePolicyRequestToJson() throws IOException {
        // Given
        final SetTypePolicyRequest request = new SetTypePolicyRequest().type("TestObj").policy(typePolicy);

        // When
        String str = this.mapper.writeValueAsString(request);

        final JsonNode node = this.mapper.readTree(str);
        final Iterable<String> iterable = node::fieldNames;

        //That
        Assertions.assertThat(iterable).as("SetTypePolicyRequest not parsed to json").isNotEmpty().doesNotContainNull().contains("id", "type", "policy");
    }

    @Test
    public void testSetTypePolicyRequestFromJson() throws IOException {
        // Given
        final SetTypePolicyRequest expected = new SetTypePolicyRequest().type("TestObj").policy(typePolicy);

        final String jsonString = "{\"id\":{\"id\":\"d75032bc-0509-44ac-8bad-51d4c5fd0fd4\"},\"type\":\"TestObj\",\"policy\":{\"recordRules\":{\"message\":\"no rules set\",\"rules\":{}},\"resourceRules\":{\"message\":\"Testing purpose\",\"rules\":{\"2f9f68f1-f311-4821-9124-e4c987c72df4\":{\"class\":\"uk.gov.gchq.palisade.policy.IsTextResourceRule\"}}},\"owner\":{\"userId\":{\"id\":\"TestUser\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";

        // When
        final SetTypePolicyRequest actual = this.mapper.readValue(jsonString, SetTypePolicyRequest.class);

        // Then
        assertAll("DeserialisingComparedToObject",
                () -> Assertions.assertThat(actual.getType()).as("SetTypePolicyRequest could not be parsed from json")
                        .isEqualTo(expected.getType()),
                () -> Assertions.assertThat(actual.getPolicy().getOwner()).as("SetTypePolicyRequest could not be parsed from json")
                        .isEqualTo(expected.getPolicy().getOwner())
        );
    }
}
