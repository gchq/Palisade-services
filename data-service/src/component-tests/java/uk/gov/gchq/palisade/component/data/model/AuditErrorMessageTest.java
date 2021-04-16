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
package uk.gov.gchq.palisade.component.data.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.data.common.data.reader.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDIT_ERROR_MESSAGE;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION;

@JsonTest
@ContextConfiguration(classes = {AuditErrorMessageTest.class})
class AuditErrorMessageTest {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    /**
     * Creates the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testAuditErrorMessageSerialisingAndDeserialising() throws IOException {
        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(AUDIT_ERROR_MESSAGE);
        ObjectContent<AuditErrorMessage> auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        AuditErrorMessage auditErrorMessageObject = auditErrorMessageObjectContent.getObject();

        assertAll("ObjectComparison",
                () -> assertThat(auditErrorMessageObject)
                        .as("Comparison assertion using the AuditErrorMessage's equals")
                        .isEqualTo(AUDIT_ERROR_MESSAGE),

                () -> assertThat(auditErrorMessageObject)
                        .as("Comparison assertion using all of the AuditErrorMessage's components recursively")
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .isEqualTo(AUDIT_ERROR_MESSAGE),

                () -> assertThat(auditErrorMessageObject.getError().getMessage())
                        .as("Comparison assertion using the Throwable's exception")
                        .isEqualTo(AUDIT_ERROR_MESSAGE.getError().getMessage())
        );
    }

    /**
     * Grouped assertion test for the scenario that the error message only has the token and leaf resource id.
     * This is a special case where the error occurs during the initial request to authenticate the data request.
     * In this scenario, the {@link AuditErrorMessage} will  be based on the  {@link DataRequest} and there is no
     * {@link DataReaderRequest}.
     * DataCreate the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditErrorMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testForFailedAuthenticationAuditErrorMessageSerialisingAndDeserialising() throws IOException {
        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION);
        ObjectContent<AuditErrorMessage> auditErrorMessageObjectContent = jsonTester.parse(auditErrorMessageJsonContent.getJson());
        AuditErrorMessage auditErrorMessageObject = auditErrorMessageObjectContent.getObject();

        assertAll("ObjectComparison",
                () -> assertThat(auditErrorMessageObject)
                        .as("Comparison assertion using the AuditErrorMessage's equals")
                        .isEqualTo(AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION),

                () -> assertThat(auditErrorMessageObject)
                        .as("Comparison assertion using all of the AuditErrorMessage's components recursively")
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .isEqualTo(AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION),

                () -> assertThat(auditErrorMessageObject.getError().getMessage())
                        .as("Assertion check of the error message")
                        .isEqualTo(AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION.getError().getMessage())
        );
    }
}
