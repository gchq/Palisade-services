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
package uk.gov.gchq.palisade.service.data.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class AuditSuccessMessageTest {

    @Autowired
    private JacksonTester<AuditSuccessMessage> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditSuccessMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void groupedDependantErrorMessageSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23");

        AuditSuccessMessage auditSuccessMessage = AuditSuccessMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withAttributes(attributes)
                .withLeafResourceId("testLeafResourceId")
                .withNumberOfRecordsProcessed(17)
                .withNumberOfRecordsReturned(4);

        JsonContent<AuditSuccessMessage> auditSuccessMessageJsonContent = jsonTester.write(auditSuccessMessage);
        ObjectContent<AuditSuccessMessage> auditSuccessMessageObjectContent = jsonTester.parse(auditSuccessMessageJsonContent.getJson());
        AuditSuccessMessage auditSuccessMessageObject = auditSuccessMessageObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("data-service"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.leafResourceId").isEqualTo("testLeafResourceId"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23"),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathNumberValue("$.attributes.RECORDS_PROCESSED").isEqualTo(17),
                        () -> assertThat(auditSuccessMessageJsonContent).extractingJsonPathNumberValue("$.attributes.RECORDS_RETURNED").isEqualTo(4)
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditSuccessMessageObject.getUserId()).isEqualTo(auditSuccessMessage.getUserId()),
                        () -> assertThat(auditSuccessMessageObject.getResourceId()).isEqualTo(auditSuccessMessage.getResourceId()),
                        () -> assertThat(auditSuccessMessageObject.getContext().getPurpose()).isEqualTo(auditSuccessMessage.getContext().getPurpose()),
                        () -> assertThat(auditSuccessMessageObject.getServiceName()).isEqualTo(auditSuccessMessage.getServiceName()),
                        () -> assertThat(auditSuccessMessageObject.getTimestamp()).isEqualTo(auditSuccessMessage.getTimestamp()),
                        () -> assertThat(auditSuccessMessageObject.getLeafResourceId()).isEqualTo(auditSuccessMessage.getLeafResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(auditSuccessMessageObjectContent.equals(auditSuccessMessage))
                )
        );
    }
}
