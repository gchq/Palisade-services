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
package uk.gov.gchq.palisade.component.policy.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.request.PolicyRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = PolicyRequestTest.class)
class PolicyRequestTest {

    @Autowired
    private JacksonTester<PolicyRequest> jacksonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link PolicyRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantPolicyRequestSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        PolicyRequest policyRequest = PolicyRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource);

        JsonContent<PolicyRequest> policyRequestJsonContent = jacksonTester.write(policyRequest);
        ObjectContent<PolicyRequest> policyRequestObjectContent = jacksonTester.parse(policyRequestJsonContent.getJson());
        PolicyRequest policyRequestMessageObject = policyRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(policyRequestMessageObject.getUser()).isEqualTo(policyRequest.getUser()),
                        () -> assertThat(policyRequestMessageObject.getContext()).isEqualTo(policyRequest.getContext()),
                        () -> assertThat(policyRequestMessageObject.getResource()).isEqualTo(policyRequest.getResource())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(policyRequestMessageObject).usingRecursiveComparison().isEqualTo(policyRequest)
                )
        );
    }
}
