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
package uk.gov.gchq.palisade.component.topicoffset.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.topicoffset.common.Context;
import uk.gov.gchq.palisade.service.topicoffset.model.AuditErrorMessage;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the evaluating the AuditErrorMessage and the related serialising to a JSon string
 * and deseralising back to an object.
 */
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
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testErrorMessageSerialisingAndDeserialising() throws IOException {
        var auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"))
                .withAttributes(Map.of("messagesSent", "23, Skidoo"))
                .withError(new InternalError("Something went wrong!"));

        var auditErrorMessageJsonContent = jsonTester.write(auditErrorMessage);
        var auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        var auditErrorMessageObject = auditErrorMessageObjectContent.getObject();

        assertThat(auditErrorMessageObject)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(auditErrorMessage);

        assertThat(auditErrorMessageObject)
                .as("Ignoring the error, check %s using recursion)", auditErrorMessage.getClass().getSimpleName())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(auditErrorMessage);

        assertThat(auditErrorMessageObject)
                .as("Extracting the exception, check it has been deserialised successfully")
                .extracting(AuditErrorMessage::getError)
                .isExactlyInstanceOf(Throwable.class)
                .extracting("Message")
                .isEqualTo(auditErrorMessage.getError().getMessage());
    }
}
