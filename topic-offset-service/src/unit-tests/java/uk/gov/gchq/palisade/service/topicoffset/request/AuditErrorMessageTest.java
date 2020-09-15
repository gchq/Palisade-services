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
package uk.gov.gchq.palisade.service.topicoffset.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import uk.gov.gchq.palisade.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Unit tests for the evaluating the AuditErrorMessage and the related seralising to a JSon string
 * and deseralising back to an object.
 */
@JsonTest
class AuditErrorMessageTest {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */

    @Test
    public void testGroupedDependantErrorMessageSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23, Skidoo");

        AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withAttributes(attributes)
                .withError(new InternalError("Something went wrong!"));

        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(auditErrorMessage);
        ObjectContent<AuditErrorMessage> auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        AuditErrorMessage auditErrorMessageObject = auditErrorMessageObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("topic-offset-service"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23, Skidoo"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.error.message").isEqualTo("Something went wrong!")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditErrorMessageObject.getUserId()).isEqualTo(auditErrorMessage.getUserId()),
                        () -> assertThat(auditErrorMessageObject.getResourceId()).isEqualTo(auditErrorMessage.getResourceId()),
                        () -> assertThat(auditErrorMessageObject.getContext().getPurpose()).isEqualTo(auditErrorMessage.getContext().getPurpose()),
                        () -> assertThat(auditErrorMessageObject.getServiceName()).isEqualTo(auditErrorMessage.getServiceName()),
                        () -> assertThat(auditErrorMessageObject.getServerIP()).isEqualTo(auditErrorMessage.getServerIP()),
                        () -> assertThat(auditErrorMessageObject.getServerHostName()).isEqualTo(auditErrorMessage.getServerHostName()),
                        () -> assertThat(auditErrorMessageObject.getTimestamp()).isEqualTo(auditErrorMessage.getTimestamp()),
                        () -> assertThat(auditErrorMessageObject.getError().getMessage()).isEqualTo(auditErrorMessage.getError().getMessage())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditErrorMessageObject.equals(auditErrorMessage))
                )
        );
    }
}
