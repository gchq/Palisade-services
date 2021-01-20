/*
 * Copyright 2021 Crown Copyright
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

import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_READER_RESPONSE;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_READER_RESPONSE_WITH_ERROR;

@JsonTest
@ContextConfiguration(classes = {AuditableDataReaderResponseTest.class})
class AuditableDataReaderResponseTest {

    @Autowired
    private JacksonTester<AuditableDataReaderResponse> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditableDataReaderRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantAuditableDataReaderRequestSerialisingAndDeserialising() throws IOException {

        JsonContent<AuditableDataReaderResponse> auditableDataReaderRequestJsonContent = jsonTester.write(AUDITABLE_DATA_READER_RESPONSE);
        ObjectContent<AuditableDataReaderResponse> auditableDataReaderRequestObjectContent = jsonTester.parse(auditableDataReaderRequestJsonContent.getJson());
        AuditableDataReaderResponse auditableDataReaderRequestObject = auditableDataReaderRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("ObjectComparison",
                        () -> assertThat(auditableDataReaderRequestObject).as("Check using equalTo").isEqualTo(AUDITABLE_DATA_READER_RESPONSE),
                        () -> assertThat(auditableDataReaderRequestObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(AUDITABLE_DATA_READER_RESPONSE)
                )
        );
    }

    @Test
    void testGroupedDependantAuditableDataReaderRequestWithErrorMessageSerialisingAndDeserialising() throws IOException {

        JsonContent<AuditableDataReaderResponse> auditableDataReaderRequestJsonContent = jsonTester.write(AUDITABLE_DATA_READER_RESPONSE_WITH_ERROR);
        ObjectContent<AuditableDataReaderResponse> auditableDataReaderRequestObjectContent = jsonTester.parse(auditableDataReaderRequestJsonContent.getJson());
        AuditableDataReaderResponse auditableDataReaderRequestObject = auditableDataReaderRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("ObjectComparison",
                        () -> assertThat(auditableDataReaderRequestObject).as("Check using equalTo").isEqualTo(AUDITABLE_DATA_READER_RESPONSE_WITH_ERROR),
                        () -> assertThat(auditableDataReaderRequestObject).as("Check using recursion").usingRecursiveComparison().ignoringFieldsOfTypes(Throwable.class).isEqualTo(AUDITABLE_DATA_READER_RESPONSE_WITH_ERROR)
                )
        );
    }
}