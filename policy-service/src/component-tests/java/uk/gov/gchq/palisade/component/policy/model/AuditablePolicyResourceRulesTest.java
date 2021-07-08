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
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceRules;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = AuditablePolicyResourceRulesTest.class)
class AuditablePolicyResourceRulesTest extends CommonTestData {

    @Autowired
    private JacksonTester<AuditablePolicyResourceRules> jsonTester;

    /**
     * Test for a {@link AuditablePolicyResourceRules} which contains a {@link PolicyRequest} and no AuditErrorMessages.
     * This is the expected state of the object after a query for the rules applicable to the resource, but before these rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceRules} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testAuditablePolicyResourceRulesSerialisingAndDeserialising() throws IOException {
        var jsonContent = jsonTester.write(POLICY_RESOURCE_RULES);
        var objectContent = jsonTester.parse(jsonContent.getJson());
        var messageObject = objectContent.getObject();

        assertThat(messageObject)
                .as("The serialised and deserialised object should match the original")
                .isEqualTo(POLICY_RESOURCE_RULES);

        assertThat(messageObject)
                .as("The serialised and deserialised object should have the same values as the original")
                .usingRecursiveComparison()
                .isEqualTo(POLICY_RESOURCE_RULES);
    }

    /**
     * Test for a {@link AuditablePolicyResourceRules} which contains a {@link PolicyRequest} and an AuditErrorMessage.
     * This is the expected state of the object after a query for the rules applicable to the resource then an error occurs when rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceRules} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testErrorAuditablePolicyResourceRulesSerialisingAndDeserialising() throws IOException {
        var jsonContent = jsonTester.write(POLICY_RESOURCE_RULES_ERROR);
        var objectContent = jsonTester.parse(jsonContent.getJson());
        var messageObject = objectContent.getObject();

        assertThat(messageObject)
                .as("The serialised and deserialised object should match the original")
                .isEqualTo(POLICY_RESOURCE_RULES_ERROR);

        assertThat(messageObject)
                .as("The serialised and deserialised object should have the same values as the original")
                .usingRecursiveComparison()
                .isEqualTo(POLICY_RESOURCE_RULES_ERROR);
    }
}
