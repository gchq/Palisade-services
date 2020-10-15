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
package uk.gov.gchq.palisade.component.data.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {AuditSuccessMessageTest.class})
class AuditSuccessMessageTest {

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
    void testGroupedDependantSuccessMessageSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");

        AuditSuccessMessage auditSuccessMessage = AuditSuccessMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withLeafResourceId("testLeafResourceId")
                .withRecordsProcessedAndReturned(17L, 4L);

        JsonContent<AuditSuccessMessage> auditSuccessMessageJsonContent = jsonTester.write(auditSuccessMessage);
        ObjectContent<AuditSuccessMessage> auditSuccessMessageObjectContent = jsonTester.parse(auditSuccessMessageJsonContent.getJson());
        AuditSuccessMessage auditSuccessMessageObject = auditSuccessMessageObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathStringValue("$.userId")
                                .isEqualTo("originalUserID"),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathStringValue("$.resourceId")
                                .isEqualTo("testResourceId"),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathStringValue("$.context.contents.purpose")
                                .isEqualTo("testContext"),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathStringValue("$.serviceName")
                                .isEqualTo("data-service"),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathStringValue("$.leafResourceId")
                                .isEqualTo("testLeafResourceId"),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathNumberValue("$.attributes.RECORDS_PROCESSED")
                                .extracting(Number::longValue)
                                .isEqualTo(17L),

                        () -> assertThat(auditSuccessMessageJsonContent)
                                .extractingJsonPathNumberValue("$.attributes.RECORDS_RETURNED")
                                .extracting(Number::longValue)
                                .isEqualTo(4L)
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(auditSuccessMessageObject.getUserId())
                                .isEqualTo(auditSuccessMessage.getUserId()),

                        () -> assertThat(auditSuccessMessageObject.getResourceId())
                                .isEqualTo(auditSuccessMessage.getResourceId()),

                        () -> assertThat(auditSuccessMessageObject.getContext().getPurpose())
                                .isEqualTo(auditSuccessMessage.getContext().getPurpose()),

                        () -> assertThat(auditSuccessMessageObject.getServiceName())
                                .isEqualTo(auditSuccessMessage.getServiceName()),

                        () -> assertThat(auditSuccessMessageObject.getTimestamp())
                                .isEqualTo(auditSuccessMessage.getTimestamp()),

                        () -> assertThat(auditSuccessMessageObject.getServerHostName())
                                .isEqualTo(auditSuccessMessage.getServerHostName()),

                        () -> assertThat(auditSuccessMessageObject.getServerIP())
                                .isEqualTo(auditSuccessMessage.getServerIP()),

                        () -> assertThat(auditSuccessMessageObject.getLeafResourceId())
                                .isEqualTo(auditSuccessMessage.getLeafResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(auditSuccessMessageObject)
                                .usingRecursiveComparison()
                                .withComparatorForType(Comparator.comparingLong(Number::longValue), Number.class)
                                .isEqualTo(auditSuccessMessage)
                )
        );
    }
}
