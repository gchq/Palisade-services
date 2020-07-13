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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@JsonTest
public class PolicyResponseTest {

    @Autowired
    private JacksonTester<PolicyResponse> jacksonTester;

    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {

        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        Rules rules = new Rules().rule("Rule1", new PassThroughRule());

        PolicyResponse policyResponse = PolicyResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource)
                .withRule(rules);

        JsonContent<PolicyResponse> policyRequestJsonContent = jacksonTester.write(policyResponse);

        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set");

    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {

        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"originalResourceID\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"user\":{\"userId\":{\"id\":\"testUserId\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/test/file.format\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{\"Rule1\":{\"class\":\"uk.gov.gchq.palisade.service.policy.response.PassThroughRule\"}}}}";
        ObjectContent<PolicyResponse> policyResponseObjectContent = jacksonTester.parse(jsonString);

        PolicyResponse policyResponse = policyResponseObjectContent.getObject();
        assertThat(policyResponse.getUserId()).isEqualTo("originalUserID");
        assertThat(policyResponse.getResourceId()).isEqualTo("originalResourceID");
        assertThat(policyResponse.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(policyResponse.getUser().getUserId().getId()).isEqualTo("testUserId");
        assertThat(policyResponse.getResource().getId()).isEqualTo("/test/file.format");
        assertThat(policyResponse.rules.getMessage()).isEqualTo("no rules set");


    }
}