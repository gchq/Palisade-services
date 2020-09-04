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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.HasTestingPurpose;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
public class SetResourcePolicyRequestTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final FileResource fileResource1 = createTestFileResource(1);
    private final User testUser = new User().userId("TestUser");
    private Policy resourcePolicy;

    @BeforeEach
    public void setup() {
        resourcePolicy = new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());
    }

    private static FileResource createTestFileResource(final int i) {
        return ((FileResource) ResourceBuilder.create("file:/temp/TestObj_00" + i + ".txt")).type("TestObj" + i).serialisedFormat("txt");
    }

    @Test
    public void testSetResourcePolicyRequestToJson() throws IOException {
        // Given
        final SetResourcePolicyRequest request = new SetResourcePolicyRequest().policy(resourcePolicy).resource(fileResource1);

        // When
        final JsonNode node = this.mapper.readTree(this.mapper.writeValueAsString(request));
        final Iterable<String> iterable = node::fieldNames;

        //That
        assertThat(iterable).isNotEmpty().doesNotContainNull().contains("id", "resource", "policy");
    }

    @Test
    public void testSetResourcePolicyRequestFromJson() throws IOException {
        //Given
        final String jsonString = "{\"id\":{\"id\":\"a423c74f-ea6c-44ed-bf21-0366e51462ce\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"File://temp/TestObj_001.txt\",\"attributes\":{},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"File://temp/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"File/\"}},\"serialisedFormat\":\"txt\",\"type\":\"TestObj1\"},\"policy\":{\"recordRules\":{\"message\":\"no rules set\",\"rules\":{}},\"resourceRules\":{\"message\":\"Purpose is testing\",\"rules\":{\"38e91906-53d4-4504-917b-46fabae6b7b8\":{\"class\":\"uk.gov.gchq.palisade.policy.HasTestingPurpose\"}}},\"owner\":{\"userId\":{\"id\":\"TestUser\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";

        // When
        final SetResourcePolicyRequest actual = this.mapper.readValue(jsonString, SetResourcePolicyRequest.class);
        Policy expected = new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());

        // Then
        assertAll("DeserialisingComparedToObject",
                () -> assertThat(actual.getPolicy().getResourceRules().getMessage()).as("SetResourcePolicyRequest could not be parsed from json string")
                        .isEqualTo(expected.getResourceRules().getMessage()),
                () -> assertThat(actual.getPolicy().getRecordRules()).as("SetResourcePolicyRequest could not be parsed from json string")
                        .isEqualTo(expected.getRecordRules()),
                () -> assertThat(actual.getPolicy().getOwner()).as("AddUserRequest could not be parsed from json string")
                        .isEqualTo(expected.getOwner())
        );
    }
}
