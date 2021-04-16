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
package uk.gov.gchq.palisade.component.user.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {AuditErrorMessageTest.class})
class AuditErrorMessageTest {

    @Autowired
    private ObjectMapper mapper;

    /**
     * Tests the creation of the message type, AuditErrorMessage using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or de-serialise the string.
     */
    @Test
    void testAuditErrorMessageSerialisingAndDeserialising() throws IOException {
        var auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"))
                .withAttributes(Map.of("messagesSent", "23"))
                .withError(new Throwable("Something went wrong!"));

        var actualJson = mapper.writeValueAsString(auditErrorMessage);
        var actualInstance = mapper.readValue(actualJson, auditErrorMessage.getClass());

        assertThat(actualInstance)
                .as("Ignoring the error, check %s using recursion", auditErrorMessage.getClass().getSimpleName())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(auditErrorMessage);

        assertThat(actualInstance)
                .as("Extracting the exception, check it has been deserialized successfully")
                .extracting(AuditErrorMessage::getError)
                .isExactlyInstanceOf(Throwable.class)
                .extracting("Message")
                .isEqualTo("Something went wrong!");

    }
}