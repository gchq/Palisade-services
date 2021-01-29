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

import uk.gov.gchq.palisade.service.data.model.AuditableDataRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;

@JsonTest
@ContextConfiguration(classes = {AuditableDataRequestTest.class})
class AuditableDataRequestTest {

    @Autowired
    private JacksonTester<AuditableDataRequest> jsonTester;


    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AuditableDataRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantAuditableDataRequestSerialisingAndDeserialising() throws IOException {
        JsonContent<AuditableDataRequest> auditableDataRequestJsonContent = jsonTester.write(AUDITABLE_DATA_REQUEST);
        ObjectContent<AuditableDataRequest> auditableDataRequestObjectContent = jsonTester.parse(auditableDataRequestJsonContent.getJson());
        AuditableDataRequest auditableDataRequestObject = auditableDataRequestObjectContent.getObject();

        assertThat(auditableDataRequestObject)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Throwable.class)
                .isEqualTo(AUDITABLE_DATA_REQUEST);
    }


    @Test
    void testGroupedDependantAuditableDataRequestWithErrorMessageSerialisingAndDeserialising() throws IOException {
        JsonContent<AuditableDataRequest> auditableDataReaderRequestJsonContent = jsonTester.write(AUDITABLE_DATA_REQUEST_WITH_ERROR);
        ObjectContent<AuditableDataRequest> auditableDataReaderRequestObjectContent = jsonTester.parse(auditableDataReaderRequestJsonContent.getJson());
        AuditableDataRequest auditableDataRequestObject = auditableDataReaderRequestObjectContent.getObject();

        assertThat(auditableDataRequestObject)
                .usingRecursiveComparison()
                .isEqualTo(AUDITABLE_DATA_REQUEST_WITH_ERROR);
    }
}