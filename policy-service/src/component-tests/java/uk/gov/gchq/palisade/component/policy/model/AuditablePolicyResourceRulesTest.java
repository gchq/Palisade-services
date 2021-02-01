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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceRules;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDIT_ERROR_MESSAGE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.REQUEST;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESOURCE_RULES;


/**
 * Unit tests for the {@link AuditablePolicyResourceRules}
 */
@JsonTest
@ContextConfiguration(classes = AuditablePolicyResourceRulesTest.class)
class AuditablePolicyResourceRulesTest {

    @Autowired
    private JacksonTester<AuditablePolicyResourceRules> jsonTester;

    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceRules} which holds a {@code PolicyRequest},
     * no exception and before the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource, but before these rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceRules} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testAuditablePolicyResourceRulesSerialisingAndDeserialising() throws IOException {
        AuditablePolicyResourceRules auditablePolicyResourceRules = AuditablePolicyResourceRules.Builder.create()
                .withPolicyRequest(REQUEST).withRules(RESOURCE_RULES).withNoErrors();

        JsonContent<AuditablePolicyResourceRules> auditablePolicyResourceRulesJsonContent = jsonTester.write(auditablePolicyResourceRules);
        ObjectContent<AuditablePolicyResourceRules> auditablePolicyResourceRulesObjectContent = jsonTester.parse(auditablePolicyResourceRulesJsonContent.getJson());
        AuditablePolicyResourceRules auditablePolicyResourceRulesObjectContentObject = auditablePolicyResourceRulesObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.policyRequest.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set"),
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.auditErrorMessage").isNull()
                ),


                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getPolicyRequest()).isEqualTo(auditablePolicyResourceRules.getPolicyRequest()),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getRules()).isEqualTo(auditablePolicyResourceRules.getRules()),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getAuditErrorMessage()).isNull(),
                        () -> assertThat(auditablePolicyResourceRules.getAuditErrorMessage()).isNull()

                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject).as("Check using EqualTo").isEqualTo(auditablePolicyResourceRules),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyResourceRules)
                )
        );
    }


    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceRules} which holds a {@code PolicyRequest},
     * has and exception and before the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource then an error occurs when rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceRules} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testErrorAuditablePolicyResourceRulesSerialisingAndDeserialising() throws IOException {
        AuditablePolicyResourceRules auditablePolicyResourceRules = AuditablePolicyResourceRules.Builder.create()
                .withPolicyRequest(REQUEST).withRules(RESOURCE_RULES).withAuditErrorMessage(AUDIT_ERROR_MESSAGE);
        JsonContent<AuditablePolicyResourceRules> auditablePolicyResourceRulesJsonContent = jsonTester.write(auditablePolicyResourceRules);
        ObjectContent<AuditablePolicyResourceRules> auditablePolicyResourceRulesObjectContent = jsonTester.parse(auditablePolicyResourceRulesJsonContent.getJson());
        AuditablePolicyResourceRules auditablePolicyResourceRulesObjectContentObject = auditablePolicyResourceRulesObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.policyRequest.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set"),
                        () -> assertThat(auditablePolicyResourceRulesJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.userId").isEqualTo("test-user-id")
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getPolicyRequest()).isEqualTo(auditablePolicyResourceRules.getPolicyRequest()),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getRules()).isEqualTo(auditablePolicyResourceRules.getRules()),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject.getAuditErrorMessage()).isEqualTo(auditablePolicyResourceRules.getAuditErrorMessage())
                ),

                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject).as("Check using EqualTo").isEqualTo(auditablePolicyResourceRules),
                        () -> assertThat(auditablePolicyResourceRulesObjectContentObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyResourceRules)
                )
        );
    }
}