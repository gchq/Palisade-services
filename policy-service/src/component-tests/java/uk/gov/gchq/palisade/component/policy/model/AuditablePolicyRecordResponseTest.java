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
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESPONSE;

@JsonTest
@ContextConfiguration(classes = AuditablePolicyRecordResponseTest.class)
class AuditablePolicyRecordResponseTest extends CommonTestData {

    @Autowired
    private JacksonTester<AuditablePolicyRecordResponse> jsonTester;

    /**
     * Grouped assertion test for a {@link AuditablePolicyRecordResponse} which holds a PolicyResponse and no exception.
     * This is the scenario where the object represents a standard message that is to be passed onto the next service.
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyRecordResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialize the string.
     */
    @Test
    void testAuditablePolicyRecordResponseSerializingAndDeserializing() throws IOException {

        JsonContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseJsonContent = jsonTester.write(POLICY_RECORD_RESPONSE);
        ObjectContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseObjectContent = jsonTester.parse(auditablePolicyRecordResponseJsonContent.getJson());
        AuditablePolicyRecordResponse auditablePolicyRecordResponseMessageObject = auditablePolicyRecordResponseObjectContent.getObject();

        assertAll("AuditablePolicyRecordResponse serializing and deserializing comparison",
                () -> assertThat(auditablePolicyRecordResponseMessageObject)
                        .as("The serialized and deserialized object should match the original")
                        .isEqualTo(POLICY_RECORD_RESPONSE),

                () -> assertThat(auditablePolicyRecordResponseMessageObject)
                        .as("The serialized and deserialized object should have the same values as the original")
                        .usingRecursiveComparison()
                        .isEqualTo(POLICY_RECORD_RESPONSE)
        );
    }

    /**
     * Grouped assertion test for a {@link AuditablePolicyRecordResponse} which holds an exception an no policy response
     * This is the scenario where the object represents a error that is to be passed onto the audit service.
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyRecordResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialize the string.
     */
    @Test
    void testAuditablePolicyRecordResponseExceptionSerializingAndDeserializing() throws IOException {

        JsonContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseJsonContent = jsonTester.write(POLICY_RECORD_RESPONSE_ERROR);
        ObjectContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseObjectContent = jsonTester.parse(auditablePolicyRecordResponseJsonContent.getJson());
        AuditablePolicyRecordResponse auditablePolicyRecordResponseMessageObject = auditablePolicyRecordResponseObjectContent.getObject();

        assertAll("AuditablePolicyRecordResponse with error serializing and deserializing comparison",
                () -> assertThat(auditablePolicyRecordResponseMessageObject)
                        .as("The serialized and deserialized object should match the original")
                        .isEqualTo(POLICY_RECORD_RESPONSE_ERROR),

                () -> assertThat(auditablePolicyRecordResponseMessageObject)
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .as("The serialized and deserialized object should have the same values as the original, ignoring the Throwable value")
                        .isEqualTo(POLICY_RECORD_RESPONSE_ERROR)
        );
    }

    /**
     * When there is no {@link AuditErrorMessage} the  {@link AuditablePolicyRecordResponse#chain(AuditErrorMessage)}
     * is expected to return the same object.
     */
    @Test
    void testChainWithoutAnException() {
        // When
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(RESPONSE).withNoErrors();
        AuditablePolicyRecordResponse chainedResponse = auditablePolicyRecordResponse.chain(null);
        // Then
        assertAll("AuditablePolicyRecordResponse comparison after chaining with 'null' error",
                () -> assertThat(chainedResponse)
                        .as("The updated object should match the original")
                        .isEqualTo(auditablePolicyRecordResponse),

                () -> assertThat(chainedResponse)
                        .usingRecursiveComparison()
                        .as("The updated object should have the same values as the original")
                        .isEqualTo(auditablePolicyRecordResponse)
        );
    }

    /**
     * When there is no {@link AuditErrorMessage} the {@link AuditablePolicyRecordResponse#chain(AuditErrorMessage)}
     * is expected to return a new and different object with the error message {@link AuditErrorMessage} added.
     */
    @Test
    void testChainWithAnException() {
        // When
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(RESPONSE).withNoErrors();
        AuditablePolicyRecordResponse chainedResponse = auditablePolicyRecordResponse.chain(AUDIT_ERROR_MESSAGE);
        // Then
        assertAll("AuditablePolicyRecordResponse comparison after chaining with an AuditErrorMessage",
                () -> assertThat(chainedResponse)
                        .usingRecursiveComparison()
                        .as("The updated object should not match the original")
                        .isNotEqualTo(auditablePolicyRecordResponse),

                () -> assertThat(chainedResponse.getAuditErrorMessage())
                        .usingRecursiveComparison()
                        .as("The AuditErrorMessage should have the expected values")
                        .isEqualTo(AUDIT_ERROR_MESSAGE),

                () -> assertThat(chainedResponse.getAuditErrorMessage())
                        .extracting(AuditErrorMessage::getError)
                        .as("The exception cause should be 'NoSuchPolicyException'")
                        .isInstanceOf(NoSuchPolicyException.class)
                        .as("The exception should contain the message 'No rules found for the resource'")
                        .extracting(Throwable::getMessage)
                        .isEqualTo("No rules found for the resource")
        );
    }
}
