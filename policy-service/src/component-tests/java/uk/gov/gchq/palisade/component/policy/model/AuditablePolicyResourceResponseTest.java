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

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Unit tests for the {@link AuditablePolicyResourceResponse}
 */
@JsonTest
@ContextConfiguration(classes = AuditablePolicyResourceResponseTest.class)
class AuditablePolicyResourceResponseTest extends CommonTestData {

    @Autowired
    private JacksonTester<AuditablePolicyResourceResponse> jsonTester;


    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceResponse} which holds a {@code PolicyRequest},
     * no exception and after the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource then these rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialize the string.
     */
    @Test
    void testAuditablePolicyResourceResponseSerializingAndDeserializing() throws IOException {

        JsonContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseJsonContent = jsonTester.write(POLICY_RESOURCE_RESPONSE);
        ObjectContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseObjectContent = jsonTester.parse(auditablePolicyResourceResponseJsonContent.getJson());
        AuditablePolicyResourceResponse auditablePolicyResourceResponseObjectContentObject = auditablePolicyResourceResponseObjectContent.getObject();

        assertAll("AuditablePolicyResourceResponse serializing and deserializing comparison",
                //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                () -> assertThat(auditablePolicyResourceResponseObjectContentObject)
                        .as("Compare the AuditablePolicyResourceResponse objects")
                        .isEqualTo(POLICY_RESOURCE_RESPONSE),

                () -> assertThat(auditablePolicyResourceResponseObjectContentObject)
                        .as("Recursively compare the AuditablePolicyResourceResponse object")
                        .usingRecursiveComparison()
                        .isEqualTo(POLICY_RESOURCE_RESPONSE)
        );
    }

    /**
     * Grouped assertion test for a {@link AuditablePolicyResourceResponse} which holds a {@code PolicyRequest},
     * has and exception and after the {@code Resource} has been modified.  This is the expected state of the object
     * after a query for the rules applicable to the resource then an error occurs when rules have been applied to the resource.
     * Test involve creating the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyResourceResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialize the string.
     */
    @Test
    void testErrorAuditablePolicyResourceResponseSerializingAndDeserializing() throws IOException {

        JsonContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseJsonContent = jsonTester.write(POLICY_RESOURCE_RESPONSE_ERROR);
        ObjectContent<AuditablePolicyResourceResponse> auditablePolicyResourceResponseObjectContent = jsonTester.parse(auditablePolicyResourceResponseJsonContent.getJson());
        AuditablePolicyResourceResponse auditablePolicyResourceResponseObjectContentObject = auditablePolicyResourceResponseObjectContent.getObject();

        assertAll("AuditablePolicyResourceResponse with error serializing and deserializing comparison",
                //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                () -> assertThat(auditablePolicyResourceResponseObjectContentObject)
                        .as("Compare the AuditablePolicyResourceResponse objects")
                        .isEqualTo(POLICY_RESOURCE_RESPONSE_ERROR),

                () -> assertThat(auditablePolicyResourceResponseObjectContentObject)
                        .as("Recursively compare the AuditablePolicyResourceResponse object, ignoring the Throwable value")
                        .usingRecursiveComparison()
                        .isEqualTo(POLICY_RESOURCE_RESPONSE_ERROR)
        );
    }
}
