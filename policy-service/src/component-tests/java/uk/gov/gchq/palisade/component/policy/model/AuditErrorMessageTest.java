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
package uk.gov.gchq.palisade.component.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.component.policy.MapperConfiguration;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {AuditErrorMessageTest.class, MapperConfiguration.class})
class AuditErrorMessageTest extends CommonTestData {

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws JsonProcessingException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testErrorMessageSerializingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var actualJson = mapper.writeValueAsString(AUDIT_ERROR_MESSAGE);
        var actualInstance = mapper.readValue(actualJson, AUDIT_ERROR_MESSAGE.getClass());

        assertThat(actualInstance)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(AUDIT_ERROR_MESSAGE);

        assertThat(actualInstance)
                .as("Ignoring the error, check %s using recursion", AUDIT_ERROR_MESSAGE.getClass().getSimpleName())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(AUDIT_ERROR_MESSAGE);

        assertThat(actualInstance)
                .as("Extracting the exception, check it has been deserialised successfully")
                .extracting(AuditErrorMessage::getError)
                .isExactlyInstanceOf(Throwable.class)
                .extracting("Message")
                .isEqualTo(AUDIT_ERROR_MESSAGE.getError().getMessage());
    }
}
