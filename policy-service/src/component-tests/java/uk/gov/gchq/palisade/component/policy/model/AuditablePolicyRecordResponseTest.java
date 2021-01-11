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

import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.AUDIT_ERROR_MESSAGE;
import static uk.gov.gchq.palisade.service.policy.ApplicationTestData.RESPONSE;

@JsonTest
@ContextConfiguration(classes = AuditablePolicyRecordResponseTest.class)
class AuditablePolicyRecordResponseTest {

    @Autowired
    private JacksonTester<AuditablePolicyRecordResponse> jsonTester;

    /**
     * Grouped assertion test for a {@link AuditablePolicyRecordResponse} which holds a PolicyResponse and no exception.
     * This is the scenario where the object represents a standard message that is to be passed onto the next service.
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyRecordResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testAuditablePolicyRecordResponseSerialisingAndDeserialising() throws IOException {
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(RESPONSE).withNoErrors();

        JsonContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseJsonContent = jsonTester.write(auditablePolicyRecordResponse);
        ObjectContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseObjectContent = jsonTester.parse(auditablePolicyRecordResponseJsonContent.getJson());
        AuditablePolicyRecordResponse auditablePolicyRecordResponseMessageObject = auditablePolicyRecordResponseObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyRecordResponseJsonContent).extractingJsonPathStringValue("$.policyResponse.userId").isEqualTo("test-user-id"),
                        () -> assertThat(auditablePolicyRecordResponseJsonContent).extractingJsonPathStringValue("$.auditErrorMessage").isNull()
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyRecordResponseMessageObject.getPolicyResponse()).isEqualTo(auditablePolicyRecordResponse.getPolicyResponse()),
                        () -> assertThat(auditablePolicyRecordResponseMessageObject.getAuditErrorMessage()).isNull(),
                        () -> assertThat(auditablePolicyRecordResponse.getAuditErrorMessage()).isNull()

                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyRecordResponseMessageObject).as("Check using EqualTo").isEqualTo(auditablePolicyRecordResponse),
                        () -> assertThat(auditablePolicyRecordResponseMessageObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyRecordResponse)

                )
        );
    }

    /**
     * Grouped assertion test for a {@link AuditablePolicyRecordResponse} which holds an exception an no policy response
     * This is the scenario where the object represents a error that is to be passed onto the audit service.
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditablePolicyRecordResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testAuditablePolicyRecordResponseExceptionSerialisingAndDeserialising() throws IOException {
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(null).withAuditErrorMessage(AUDIT_ERROR_MESSAGE);
        JsonContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseJsonContent = jsonTester.write(auditablePolicyRecordResponse);
        ObjectContent<AuditablePolicyRecordResponse> auditablePolicyRecordResponseObjectContent = jsonTester.parse(auditablePolicyRecordResponseJsonContent.getJson());
        AuditablePolicyRecordResponse auditablePolicyRecordResponseMessageObject = auditablePolicyRecordResponseObjectContent.getObject();
        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditablePolicyRecordResponseJsonContent).extractingJsonPathStringValue("$.policyResponse").isNull(),
                        () -> assertThat(auditablePolicyRecordResponseJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.userId").isEqualTo("test-user-id")
                ),

                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditablePolicyRecordResponseMessageObject.getPolicyResponse()).isNull(),
                        () -> assertThat(auditablePolicyRecordResponse.getPolicyResponse()).isNull(),
                        () -> assertThat(auditablePolicyRecordResponseMessageObject.getAuditErrorMessage()).isEqualTo(auditablePolicyRecordResponse.getAuditErrorMessage())


                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditablePolicyRecordResponseMessageObject).as("Check using EqualTo").isEqualTo(auditablePolicyRecordResponse),
                        () -> assertThat(auditablePolicyRecordResponseMessageObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(auditablePolicyRecordResponse)

                )
        );
    }

    /**
     * When there is no {@link AuditErrorMessage} the  {@link AuditablePolicyRecordResponse#chain(AuditErrorMessage)}
     * is expected to return the same object.
     */
    @Test
    void testChainWithoutAnException() {
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(RESPONSE).withNoErrors();
        AuditablePolicyRecordResponse chainedResponse = auditablePolicyRecordResponse.chain(null);
        //same object
        assertThat(chainedResponse).usingRecursiveComparison().isEqualTo(auditablePolicyRecordResponse);
        assertThat(chainedResponse).isEqualTo(auditablePolicyRecordResponse);
    }

    /**
     * When there is no {@link AuditErrorMessage} the  {@link AuditablePolicyRecordResponse#chain(AuditErrorMessage)}
     * is expected to return a new and different object with the error message {@link AuditErrorMessage} added.
     */
    @Test
    void testChainWitAnException() {
        AuditablePolicyRecordResponse auditablePolicyRecordResponse = AuditablePolicyRecordResponse.Builder.create().withPolicyResponse(RESPONSE).withNoErrors();
        AuditablePolicyRecordResponse chainedResponse = auditablePolicyRecordResponse.chain(AUDIT_ERROR_MESSAGE);
        assertThat(chainedResponse).usingRecursiveComparison().isNotEqualTo(auditablePolicyRecordResponse);
        assertThat(chainedResponse.getAuditErrorMessage()).usingRecursiveComparison().isEqualTo(AUDIT_ERROR_MESSAGE);
        assertThat(chainedResponse.getAuditErrorMessage()).isEqualTo(AUDIT_ERROR_MESSAGE);
    }
}
