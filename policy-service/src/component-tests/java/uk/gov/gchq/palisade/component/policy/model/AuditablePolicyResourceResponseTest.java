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
package uk.gov.gchq.palisade.component.policy.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDIT_ERROR_MESSAGE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.LEAF_RESOURCE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.REQUEST;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RULES;

/**
 * Unit tests for the {@link AuditablePolicyResourceResponse}
 */
@JsonTest
@ContextConfiguration(classes = AuditablePolicyResourceResponseTest.class)
class AuditablePolicyResourceResponseTest {

    @Autowired
    private JacksonTester<AuditablePolicyResourceResponse> jsonTester;

    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceResponse} which holds a {@code PolicyRequest},
     * no exception and before the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource, but before these rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testInitalAuditablePolicyResourceResponseSerialisingAndDeserialising() throws IOException {
        AuditablePolicyResourceResponse auditablePolicyResourceResponse = AuditablePolicyResourceResponse.Builder.create()
                .withPolicyRequest(REQUEST).withRules(RULES).withNoErrors().withNoNoModifiedResponse();

        JsonContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseJsonContent = jsonTester.write(auditablePolicyResourceResponse);
        ObjectContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseObjectContent = jsonTester.parse(auditablePolicyResourceResponseJsonContent.getJson());
        AuditablePolicyResourceResponse auditablePolicyResourceResponseObjectContentObject = auditablePolicyResourceResponseObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.policyRequest.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.auditErrorMessage").isNull(),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.modifiedResource").isNull()
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getPolicyRequest()).isEqualTo(auditablePolicyResourceResponse.getPolicyRequest()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getRules()).isEqualTo(auditablePolicyResourceResponse.getRules()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getModifiedResource()).isNull(),
                        () -> assertThat(auditablePolicyResourceResponse.getModifiedResource()).isNull(),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getAuditErrorMessage()).isNull(),
                        () -> assertThat(auditablePolicyResourceResponse.getAuditErrorMessage()).isNull()

                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using EqualTo").isEqualTo(auditablePolicyResourceResponse),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyResourceResponse)
                )
        );
    }

    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceResponse} which holds a {@code PolicyRequest},
     * no exception and after the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource then these rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testModifiedAuditablePolicyResourceResponseSerialisingAndDeserialising() throws IOException {
        AuditablePolicyResourceResponse auditablePolicyResourceResponse = AuditablePolicyResourceResponse.Builder.create()
                .withPolicyRequest(REQUEST).withRules(RULES).withNoErrors().withModifiedResource(LEAF_RESOURCE);

        JsonContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseJsonContent = jsonTester.write(auditablePolicyResourceResponse);
        ObjectContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseObjectContent = jsonTester.parse(auditablePolicyResourceResponseJsonContent.getJson());
        AuditablePolicyResourceResponse auditablePolicyResourceResponseObjectContentObject = auditablePolicyResourceResponseObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.policyRequest.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.auditErrorMessage").isNull(),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.modifiedResource.id").isEqualTo("/test/resourceId")
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getPolicyRequest()).isEqualTo(auditablePolicyResourceResponse.getPolicyRequest()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getRules()).isEqualTo(auditablePolicyResourceResponse.getRules()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getModifiedResource()).isEqualTo(LEAF_RESOURCE),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getAuditErrorMessage()).isNull(),
                        () -> assertThat(auditablePolicyResourceResponse.getAuditErrorMessage()).isNull()

                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using EqualTo").isEqualTo(auditablePolicyResourceResponse),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyResourceResponse)

                )
        );
    }

    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceResponse} which holds a {@code PolicyRequest},
     * has and exception and after the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource then an error occurs when rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testErrorAuditablePolicyResourceResponseSerialisingAndDeserialising() throws IOException {
        AuditablePolicyResourceResponse auditablePolicyResourceResponse = AuditablePolicyResourceResponse.Builder.create()
                .withPolicyRequest(REQUEST).withRules(RULES).withAuditErrorMessage(AUDIT_ERROR_MESSAGE).withModifiedResource(LEAF_RESOURCE);
        JsonContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseJsonContent = jsonTester.write(auditablePolicyResourceResponse);
        ObjectContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseObjectContent = jsonTester.parse(auditablePolicyResourceResponseJsonContent.getJson());
        AuditablePolicyResourceResponse auditablePolicyResourceResponseObjectContentObject = auditablePolicyResourceResponseObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.policyRequest.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceResponseJsonContent).extractingJsonPathStringValue("$.modifiedResource.id").isEqualTo("/test/resourceId")
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getPolicyRequest()).isEqualTo(auditablePolicyResourceResponse.getPolicyRequest()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getRules()).isEqualTo(auditablePolicyResourceResponse.getRules()),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getModifiedResource()).isEqualTo(LEAF_RESOURCE),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject.getAuditErrorMessage()).isEqualTo(auditablePolicyResourceResponse.getAuditErrorMessage())
                ),

                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using EqualTo").isEqualTo(auditablePolicyResourceResponse),
                        () -> assertThat(auditablePolicyResourceResponseObjectContentObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyResourceResponse)
                )
        );
    }

}
