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
     * plus tests the serializing to a Json string and de-serializing to an object.
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or de-serialize the string.
     */
    @Test
    void testAuditErrorMessageSerialisingAndDeserialising() throws IOException {
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

        assertAll("AuditErrorMessage Serialising and Deseralising Comparison",
                () -> assertAll("AuditErrorMessage Serialising Compared To String",
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("palisade-service"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.error.message").isEqualTo("Something went wrong!")
                ),
                () -> assertAll("AuditErrorMessage Deserialising Compared To Object",
                        () -> assertThat(auditErrorMessageObject.getUserId()).isEqualTo(auditErrorMessage.getUserId()),
                        () -> assertThat(auditErrorMessageObject.getResourceId()).isEqualTo(auditErrorMessage.getResourceId()),
                        () -> assertThat(auditErrorMessageObject.getContext()).isEqualTo(auditErrorMessage.getContext()),
                        () -> assertThat(auditErrorMessageObject.getServiceName()).isEqualTo(auditErrorMessage.getServiceName()),
                        () -> assertThat(auditErrorMessageObject.getTimestamp()).isEqualTo(auditErrorMessage.getTimestamp()),
                        () -> assertThat(auditErrorMessageObject.getServerHostname()).isEqualTo(auditErrorMessage.getServerHostname()),
                        () -> assertThat(auditErrorMessageObject.getServerIP()).isEqualTo(auditErrorMessage.getServerIP()),
                        () -> assertThat(auditErrorMessageObject.getError().getMessage()).isEqualTo(auditErrorMessage.getError().getMessage())
                        // Note Throwable equals does not override Object's equal so two Throwables are only equal if they are the same instance of an object.
                ),
                () -> assertAll("Object Comparison",
                        () -> assertThat(auditErrorMessageObject).usingRecursiveComparison().ignoringFieldsOfTypes(Throwable.class).isEqualTo(auditErrorMessage)
                )
        );
    }
}