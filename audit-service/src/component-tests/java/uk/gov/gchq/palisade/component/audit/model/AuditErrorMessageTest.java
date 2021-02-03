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
package uk.gov.gchq.palisade.component.audit.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {AuditErrorMessageTest.class})
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
    void testGroupedDependantErrorMessageSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23");

        AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withServiceName("testServicename")
                .withTimestamp(now)
                .withServerIp("testServerIP")
                .withServerHostname("testServerHostname")
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
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("testServicename"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.timestamp").isEqualTo(now),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serverIP").isEqualTo("testServerIP"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serverHostname").isEqualTo("testServerHostname"),
                        () -> assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditErrorMessageObject.getUserId()).isEqualTo(auditErrorMessage.getUserId()),
                        () -> assertThat(auditErrorMessageObject.getResourceId()).isEqualTo(auditErrorMessage.getResourceId()),
                        () -> assertThat(auditErrorMessageObject.getContext().getPurpose()).isEqualTo(auditErrorMessage.getContext().getPurpose()),
                        () -> assertThat(auditErrorMessageObject.getServiceName()).isEqualTo(auditErrorMessage.getServiceName()),
                        () -> assertThat(auditErrorMessageObject.getServerIP()).isEqualTo(auditErrorMessage.getServerIP()),
                        () -> assertThat(auditErrorMessageObject.getServerHostname()).isEqualTo(auditErrorMessage.getServerHostname()),
                        () -> assertThat(auditErrorMessageObject.getTimestamp()).isEqualTo(auditErrorMessage.getTimestamp()),
                        () -> assertThat(auditErrorMessageObject.getErrorNode().get("message").asText())
                                .isEqualTo(auditErrorMessage.getErrorNode().get("message").asText())
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(auditErrorMessageObject).usingRecursiveComparison().isEqualTo(auditErrorMessage)
                )
        );
    }
}
