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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.policy.CommonTestData;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = AuditErrorMessageTest.class)
class AuditErrorMessageTest extends CommonTestData {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialize the string.
     */
    @Test
    void testGroupedDependantErrorMessageSerializingAndDeserializing() throws IOException {

        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(AUDIT_ERROR_MESSAGE);
        ObjectContent<AuditErrorMessage> auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        AuditErrorMessage auditErrorMessageObject = auditErrorMessageObjectContent.getObject();


        assertAll("AuditErrorMessage serializing and deserializing comparison",
                () -> assertThat(auditErrorMessageObject)
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .as("Recursively compare the AuditErrorMessage object, ignoring the Throwable value")
                        .isEqualTo(AUDIT_ERROR_MESSAGE),

                () -> assertThat(auditErrorMessageObject)
                        .extracting(AuditErrorMessage::getError)
                        .as("Check that there is an error value")
                        .isNotNull()
                        .extracting(Throwable::getMessage)
                        .as("Check the message of the thrown error")
                        .isEqualTo(AUDIT_ERROR_MESSAGE.getError().getMessage())
        );
    }
}
