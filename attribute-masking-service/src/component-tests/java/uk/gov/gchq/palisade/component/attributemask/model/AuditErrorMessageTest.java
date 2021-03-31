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
package uk.gov.gchq.palisade.component.attributemask.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.model.AuditErrorMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditErrorMessageTest {


    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws JsonProcessingException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantErrorMessageSerialisingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"))
                .withAttributes(Map.of("messagesSent", "23"))
                .withError(new Throwable("Something went wrong!"));

        var actualJson = mapper.writeValueAsString(auditErrorMessage);
        var actualInstance = mapper.readValue(actualJson, auditErrorMessage.getClass());

        assertThat(actualInstance)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(auditErrorMessage);

        assertThat(actualInstance)
                .as("Ignoring the error, check %s using recursion)", auditErrorMessage.getClass().getSimpleName())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(auditErrorMessage);

        assertThat(actualInstance)
                .as("Extracting the exception, check it has been deserialised successfully")
                .extracting(AuditErrorMessage::getError)
                .isExactlyInstanceOf(Throwable.class)
                .extracting("Message")
                .isEqualTo(auditErrorMessage.getError().getMessage());
    }
}
