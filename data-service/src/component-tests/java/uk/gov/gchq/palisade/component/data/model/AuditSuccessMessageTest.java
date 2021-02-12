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

import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDIT_SUCCESS_MESSAGE;

@JsonTest
@ContextConfiguration(classes = {AuditSuccessMessageTest.class})
class AuditSuccessMessageTest {

    @Autowired
    private JacksonTester<AuditSuccessMessage> jacksonTester;

    /**
     * Creates the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditSuccessMessage} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantDataRequestReaderSerialisingAndDeserialising() throws IOException {
        JsonContent<AuditSuccessMessage> auditSuccessMessageJsonContent = jacksonTester.write(AUDIT_SUCCESS_MESSAGE);
        ObjectContent<AuditSuccessMessage> auditSuccessMessageObjectContent = jacksonTester.parse(auditSuccessMessageJsonContent.getJson());
        AuditSuccessMessage auditSuccessMessageObjectContentObject = auditSuccessMessageObjectContent.getObject();

        assertAll("ObjectComparison",
                () -> assertThat(auditSuccessMessageObjectContentObject)
                        .as("Comparison assertion using the AuditSuccessMessage's equals")
                        .isEqualTo(AUDIT_SUCCESS_MESSAGE),
                () -> assertThat(auditSuccessMessageObjectContentObject)
                        .as("Comparison assertion using all of the AuditSuccessMessage's components recursively")
                        .usingRecursiveComparison().isEqualTo(AUDIT_SUCCESS_MESSAGE)
        );
    }
}
