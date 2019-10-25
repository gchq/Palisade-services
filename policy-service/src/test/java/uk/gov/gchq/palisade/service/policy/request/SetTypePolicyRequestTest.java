/*
 * Copyright 2019 Crown Copyright
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class SetTypePolicyRequestTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetTypePolicyRequest.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private User testUser = new User().userId("TestUser");
    private Policy typePolicy;

    @Before
    public void setup() {
        typePolicy = new Policy().owner(testUser).resourceLevelRule("Testing purpose", new IsTextResourceRule());
    }

    @Test
    public void SetTypePolicyRequestToJsonTest() throws IOException{

        // Given
        final SetTypePolicyRequest request = new SetTypePolicyRequest().type("TestObj").policy(typePolicy);

        // When
        String str = this.mapper.writeValueAsString(request);

        final JsonNode node = this.mapper.readTree(str);
        final Iterable<String> iterable = node::fieldNames;

        // Then
        assertThat("SetTypePolicyRequest not parsed to json",
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")),
                is(equalTo("id, type, policy")));
    }

    @Test
    public void SetTypePolicyRequestFromJsonTest() throws IOException {

        // Given
        final SetTypePolicyRequest request = new SetTypePolicyRequest().type("TestObj").policy(typePolicy);

        final String jsonString = "{\"id\":{\"id\":\"d75032bc-0509-44ac-8bad-51d4c5fd0fd4\"},\"type\":\"TestObj\",\"policy\":{\"recordRules\":{\"message\":\"no rules set\",\"rules\":{}},\"resourceRules\":{\"message\":\"Testing purpose\",\"rules\":{\"2f9f68f1-f311-4821-9124-e4c987c72df4\":{\"class\":\"uk.gov.gchq.palisade.policy.IsTextResourceRule\"}}},\"owner\":{\"userId\":{\"id\":\"TestUser\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";
        final String asNode = this.mapper.readTree(this.mapper.writeValueAsString(request)).toString();

        // When
        final SetTypePolicyRequest result = this.mapper.readValue(jsonString, SetTypePolicyRequest.class);

        // Then
        assertThat("SetTypePolicyRequest could not be parsed from json",
                result.getType(),
                equalTo(new SetTypePolicyRequest().type("TestObj").getType()));
        assertThat("SetTypePolicyRequest could not be parsed from json",
                result.getPolicy().getOwner(),
                equalTo(new User().userId("TestUser")));
    }
}
