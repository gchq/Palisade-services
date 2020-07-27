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
import uk.gov.gchq.palisade.service.policy.request.PolicyResponse.Builder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class PolicyResponseTest {

    @Autowired
    private JacksonTester<PolicyResponse> jacksonTester;

    @Test
    public void testDeserialisePolicyResponseUsingToJson() throws IOException {
        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"originalResourceID\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\"" +
                ":{\"purpose\":\"testContext\"}},\"user\":{\"userId\":{\"id\":\"testUserId\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\"" +
                ":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/test/file.format\",\"attributes\":{},\"connectionDetail\":{\"class\":\"" +
                "uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\"" +
                ",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{}}}";

        ObjectContent<PolicyResponse> policyResponseObjectContent = jacksonTester.parse(jsonString);

        PolicyResponse policyResponse = policyResponseObjectContent.getObject();
        assertThat(policyResponse.getUserId()).isEqualTo("originalUserID");
        assertThat(policyResponse.getResourceId()).isEqualTo("originalResourceID");
        assertThat(policyResponse.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(policyResponse.getUser().getUserId().getId()).isEqualTo("testUserId");
        assertThat(policyResponse.getResource().getId()).isEqualTo("/test/file.format");
        assertThat(policyResponse.rules.getMessage()).isEqualTo("no rules set");
    }

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link PolicyResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void groupedDependantPolicyResponseSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        Rules rules = new Rules<>();

        PolicyResponse policyResponse = Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource)
                .withRule(rules);

        JsonContent<PolicyResponse> policyResponseJsonContent = jacksonTester.write(policyResponse);
        ObjectContent<PolicyResponse> policyResponseObjectContent = jacksonTester.parse(policyResponseJsonContent.getJson());
        PolicyResponse policyResponseMessageObject = policyResponseObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format"),
                        () -> assertThat(policyResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(policyResponseMessageObject.getUser()).isEqualTo(policyResponse.getUser()),
                        () -> assertThat(policyResponseMessageObject.getContext()).isEqualTo(policyResponse.getContext()),
                        () -> assertThat(policyResponseMessageObject.getResource()).isEqualTo(policyResponse.getResource()),
                        () -> assertThat(policyResponseMessageObject.rules).isEqualTo(policyResponse.rules)
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(policyResponseMessageObject.equals(policyResponse))
                )
        );
    }
}
