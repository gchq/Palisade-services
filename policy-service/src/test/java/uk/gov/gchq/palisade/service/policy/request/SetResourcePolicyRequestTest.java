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
import uk.gov.gchq.palisade.policy.HasTestingPurpose;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class SetResourcePolicyRequestTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetResourcePolicyRequestTest.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final FileResource fileResource1 = createTestFileResource(1);
    private User testUser = new User().userId("TestUser");
    private Policy resourcePolicy;

    @Before
    public void setup() {
        resourcePolicy = new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());
    }

    @Test
    public void SetResourcePolicyRequestToJsonTest() throws IOException {

        // Given
        final SetResourcePolicyRequest request = new SetResourcePolicyRequest().policy(resourcePolicy).resource(fileResource1);

        // When
        final JsonNode node = this.mapper.readTree(this.mapper.writeValueAsString(request));
        final Iterable<String> iterable = node::fieldNames;

        // Then
        assertThat("SetResourcePolicyRequest not parsed to json",
                StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")),
                is(equalTo("id, resource, policy")));
    }

    @Test
    public void SetResourcePolicyRequestFromJsonTest() throws IOException {

        // Given
        final SetResourcePolicyRequest request = new SetResourcePolicyRequest().policy(resourcePolicy).resource(fileResource1);

        final String jsonString = "{\"id\":{\"id\":\"a423c74f-ea6c-44ed-bf21-0366e51462ce\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"File://temp/TestObj_001.txt\",\"attributes\":{},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"File://temp/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"File/\"}},\"serialisedFormat\":\"txt\",\"type\":\"TestObj1\"},\"policy\":{\"recordRules\":{\"message\":\"no rules set\",\"rules\":{}},\"resourceRules\":{\"message\":\"Purpose is testing\",\"rules\":{\"38e91906-53d4-4504-917b-46fabae6b7b8\":{\"class\":\"uk.gov.gchq.palisade.policy.HasTestingPurpose\"}}},\"owner\":{\"userId\":{\"id\":\"TestUser\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";
        final String asNode = this.mapper.readTree(this.mapper.writeValueAsString(request)).toString();

        // When
        final SetResourcePolicyRequest result = this.mapper.readValue(jsonString, SetResourcePolicyRequest.class);

        // Then
        assertThat("SetResourcePolicyRequest could not be parsed from json string",
                result.getPolicy().getResourceRules().getMessage(),
                equalTo(new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>()).getResourceRules().getMessage()));
        assertThat("SetResourcePolicyRequest could not be parsed from json string",
                result.getPolicy().getRecordRules(),
                equalTo(new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>()).getRecordRules()));
        assertThat("AddUserRequest could not be parsed from json string",
                result.getPolicy().getOwner(),
                is(equalTo(new User().userId("TestUser"))));
    }

    private static FileResource createTestFileResource(final int i) {
        return ((FileResource) ResourceBuilder.create("file:/temp/TestObj_00" + i + ".txt")).type("TestObj" + i).serialisedFormat("txt");
    }
}

