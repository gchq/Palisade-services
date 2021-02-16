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

import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.AUDITABLE_DATA_REQUEST_WITH_ERROR;

class AuditableAuthorisedDataRequestTest {

    /**
     * Creates an {@code AuditableAuthorisedDataRequest} for a successful request using the class's Builder and then
     * compares it to the original object.
     */
    @Test
    void testAuditableDataRequestBuilder() {

        AuditableAuthorisedDataRequest auditableAuthorisedDataRequestObject = AuditableAuthorisedDataRequest.Builder.create()
                .withDataRequest(AUDITABLE_DATA_REQUEST.getDataRequest())
                .withAuthorisedData(AUDITABLE_DATA_REQUEST.getAuthorisedDataRequest());

        assertAll("ObjectComparison",
                () -> assertThat(auditableAuthorisedDataRequestObject)
                        .as("Comparison assertion using the AuditableDataRequest's equals")
                        .isEqualTo(AUDITABLE_DATA_REQUEST),

                () -> assertThat(auditableAuthorisedDataRequestObject)
                        .as("Comparison assertion using all of the AuditableDataRequest's components recursively")
                        .usingRecursiveComparison()
                        .isEqualTo(AUDITABLE_DATA_REQUEST)
        );
    }

    /**
     * Creates an {@code AuditableAuthorisedDataRequest} for an unsuccessful request using the class's Builder and then
     * compares it to the original object.
     */
    @Test
    void testAuditableDataRequestBuilderWithErrorMessage() {
        AuditableAuthorisedDataRequest auditableAuthorisedDataRequestObject = AuditableAuthorisedDataRequest.Builder.create()
                .withDataRequest(AUDITABLE_DATA_REQUEST_WITH_ERROR.getDataRequest())
                .withAuditErrorMessage(AUDITABLE_DATA_REQUEST_WITH_ERROR.getAuditErrorMessage());

        assertAll("ObjectComparison",
                () -> assertThat(auditableAuthorisedDataRequestObject)
                        .as("Comparison using the AuditableDataRequest's equals method")
                        .isEqualTo(AUDITABLE_DATA_REQUEST_WITH_ERROR),

                () -> assertThat(auditableAuthorisedDataRequestObject)
                        .as("Comparison of content using all of the AuditableDataRequest's components recursively")
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .isEqualTo(AUDITABLE_DATA_REQUEST_WITH_ERROR)

        );
    }
}
