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
package uk.gov.gchq.palisade.service.results.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import uk.gov.gchq.palisade.Context;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
public class AuditSuccessMessageTest {

    @Autowired
    private JacksonTester<AuditSuccessMessage> jsonTester;

    /**
     * Tests the creation of the message type, AuditSuccessMessage using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link AuditSuccessMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or de-serialise the string.
     */
    @Test
    public void testSerialiseAuditSuccessMessageToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23");
        AuditSuccessMessage auditSuccessMessage = AuditSuccessMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withAttributes(attributes)
                .withLeafResourceId("testLeafResourceId");

        JsonContent<AuditSuccessMessage> auditSuccessMessageJsonContent = jsonTester.write(auditSuccessMessage);
        ObjectContent<AuditSuccessMessage> auditSuccessMessageObjectContent = jsonTester.parse(auditSuccessMessageJsonContent.getJson());
        AuditSuccessMessage auditSuccessMessageObject = auditSuccessMessageObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("results-service"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.leafResourceId").isEqualTo("testLeafResourceId")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditSuccessMessage.getUserId()).isEqualTo(auditSuccessMessageObject.getUserId()),
                        () -> assertThat(auditSuccessMessage.getResourceId()).isEqualTo(auditSuccessMessageObject.getResourceId()),
                        () -> assertThat(auditSuccessMessage.getContext()).isEqualTo(auditSuccessMessageObject.getContext()),
                        () -> assertThat(auditSuccessMessage.getServiceName()).isEqualTo(auditSuccessMessageObject.getServiceName()),
                        () -> assertThat(auditSuccessMessage.getTimestamp()).isEqualTo(auditSuccessMessageObject.getTimestamp()),
                        () -> assertThat(auditSuccessMessage.getServerHostName()).isEqualTo(auditSuccessMessageObject.getServerHostName()),
                        () -> assertThat(auditSuccessMessage.getServerIP()).isEqualTo(auditSuccessMessageObject.getServerIP()),
                        () -> assertThat(auditSuccessMessage.getLeafResourceId()).isEqualTo(auditSuccessMessageObject.getLeafResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(auditSuccessMessageObject).usingRecursiveComparison().isEqualTo(auditSuccessMessage)
                )
        );
    }
}

