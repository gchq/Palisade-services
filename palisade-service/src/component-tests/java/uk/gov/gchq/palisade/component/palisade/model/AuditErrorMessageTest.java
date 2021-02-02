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
package uk.gov.gchq.palisade.component.palisade.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {AuditErrorMessageTest.class})
class AuditErrorMessageTest {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    @Test
    void testContextLoads() {
        assertThat(jsonTester).isNotNull();
    }

    /**
     * Tests the creation of the message type, AuditErrorMessage using the builder
     * plus tests the serializing to a Json string and deserializing to an object.
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testAuditErrorMessageSerializingAndDeserializing() throws IOException {
        Context context = new Context().purpose("testContext");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23");

        AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withAttributes(attributes)
                .withError(new InternalError("Something went wrong!"));

        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(auditErrorMessage);
        ObjectContent<AuditErrorMessage> auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        AuditErrorMessage auditErrorMessageObject = auditErrorMessageObjectContent.getObject();

        assertAll("AuditErrorMessage serializing and deserializing comparison",
                () -> assertAll("AuditErrorMessage serializing compared to string",
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.userId")
                                .as("Check the serialized userId value").isEqualTo("originalUserID"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.resourceId")
                                .as("Check the serialized resourceId value").isEqualTo("testResourceId"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose")
                                .as("Check the serialized context value").isEqualTo("testContext"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serviceName")
                                .as("Check the serialized serviceName value").isEqualTo("palisade-service"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent")
                                .as("Check the serialized messageSent attribute value").isEqualTo("23"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.error.message")
                                .as("Check the serialized error message value").isEqualTo("Something went wrong!")
                ),
                () -> assertAll("AuditErrorMessage deserializing compared to object",
                        () -> assertThat(auditErrorMessageObject.getUserId()).as("Check the deserialized userId value")
                                .isEqualTo(auditErrorMessage.getUserId()),
                        () -> assertThat(auditErrorMessageObject.getResourceId()).as("Check the deserialized resourceId value")
                                .isEqualTo(auditErrorMessage.getResourceId()),
                        () -> assertThat(auditErrorMessageObject.getContext()).as("Check the deserialized context value")
                                .isEqualTo(auditErrorMessage.getContext()),
                        () -> assertThat(auditErrorMessageObject.getServiceName()).as("Check the deserialized serviceName value")
                                .isEqualTo(auditErrorMessage.getServiceName()),
                        () -> assertThat(auditErrorMessageObject.getTimestamp()).as("Check the deserialized timeStamp value")
                                .isEqualTo(auditErrorMessage.getTimestamp()),
                        () -> assertThat(auditErrorMessageObject.getServerHostname()).as("Check the deserialized serverHostname value")
                                .isEqualTo(auditErrorMessage.getServerHostname()),
                        () -> assertThat(auditErrorMessageObject.getServerIP()).as("Check the deserialized serverIP value")
                                .isEqualTo(auditErrorMessage.getServerIP()),
                        () -> assertThat(auditErrorMessageObject.getError().getMessage()).as("Check the deserialized error message value")
                                .isEqualTo(auditErrorMessage.getError().getMessage())
                        // Note Throwable equals does not override Object's equal so two Throwables are only equal if they are the same instance of an object.
                ),
                () -> assertAll("Object comparison",
                        () -> assertThat(auditErrorMessageObject).usingRecursiveComparison()
                                .ignoringFieldsOfTypes(Throwable.class)
                                .as("Recursively compare the AuditErrorMessage object, ignoring the Throwable value")
                                .isEqualTo(auditErrorMessage)
                )
        );
    }
}