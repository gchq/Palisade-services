/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.component.palisade.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.palisade.CommonTestData;
import uk.gov.gchq.palisade.service.palisade.model.AuditablePalisadeResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = PalisadeRequestTest.class)
class AuditablePalisadeResponseTest extends CommonTestData {

    @Autowired
    private JacksonTester<AuditablePalisadeResponse> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original response Object.
     *
     * @throws IOException throws if the {@link AuditablePalisadeResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testAuditablePalisadeRequestWithRequestSerialisingAndDeserialising() throws IOException {

        JsonContent<AuditablePalisadeResponse> requestJsonContent = jsonTester.write(AUDITABLE_WITH_REQUEST);
        ObjectContent<AuditablePalisadeResponse> requestObjectContent = jsonTester.parse(requestJsonContent.getJson());
        AuditablePalisadeResponse requestObject = requestObjectContent.getObject();

        assertAll("AuditablePalisadeResponse with request Serialising and Deseralising Comparison",
                () -> assertAll("AuditablePalisadeResponse, with PalisadeRequest, Serialising Compared To String",
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.palisadeRequest.userId").isEqualTo("testUserId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.palisadeRequest.resourceId").isEqualTo("/test/resourceId"),
                        () -> assertThat(requestJsonContent).extractingJsonPathStringValue("$.palisadeRequest.context.contents.purpose").isEqualTo("testContext")
                ),
                () -> assertAll("AuditablePalisadeResponse, with PalisadeRequest, Deserialising Compared To Object",
                        () -> assertThat(requestObject.getPalisadeResponse().getUserId()).isEqualTo(AUDITABLE_WITH_REQUEST.getPalisadeResponse().getUserId()),
                        () -> assertThat(requestObject.getPalisadeResponse().getResourceId()).isEqualTo(AUDITABLE_WITH_REQUEST.getPalisadeResponse().getResourceId()),
                        () -> assertThat(requestObject.getPalisadeResponse().getContext().getPurpose()).isEqualTo(AUDITABLE_WITH_REQUEST.getPalisadeResponse().getContext().getPurpose())
                ),
                () -> assertAll("Object Comparison",
                        //compares the two objects using the objects equal method
                        () -> assertThat(requestObject).usingRecursiveComparison().isEqualTo(AUDITABLE_WITH_REQUEST)
                )
        );
    }

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserializes and tests against the original response Object.
     *
     * @throws IOException throws if the {@link AuditablePalisadeResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the string.
     */
    @Test
    void testAuditablePalisadeRequestWithErrorSerialisingAndDeserialising() throws IOException {

        JsonContent<AuditablePalisadeResponse> errorJsonContent = jsonTester.write(AUDITABLE_WITH_ERROR);
        ObjectContent<AuditablePalisadeResponse> errorObjectContent = jsonTester.parse(errorJsonContent.getJson());
        AuditablePalisadeResponse errorObject = errorObjectContent.getObject();

        assertAll("AuditablePalisadeResponse with error Serialising and Deseralising Comparison",
                () -> assertAll("AuditablePalisadeResponse, with AuditErrorMessage, Serialising Compared To String",
                        () -> assertThat(errorJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.userId").isEqualTo("testUserId"),
                        () -> assertThat(errorJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.resourceId").isEqualTo("/test/resourceId"),
                        () -> assertThat(errorJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(errorJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.serviceName").isEqualTo("palisade-service"),
                        () -> assertThat(errorJsonContent).extractingJsonPathStringValue("$.auditErrorMessage.error.message").isEqualTo("An error")
                ),
                () -> assertAll("AuditablePalisadeResponse, with AuditErrorMessage, Deserialising Compared To Object",
                        () -> assertThat(errorObject.getAuditErrorMessage().getUserId()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getUserId()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getResourceId()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getResourceId()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getContext().getPurpose()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getContext().getPurpose()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getServiceName()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getServiceName()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getTimestamp()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getTimestamp()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getServerHostname()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getServerHostname()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getServerIP()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getServerIP()),
                        () -> assertThat(errorObject.getAuditErrorMessage().getError().getMessage()).isEqualTo(AUDITABLE_WITH_ERROR.getAuditErrorMessage().getError().getMessage())
                ),
                () -> assertAll("Object Comparison",
                        () -> assertThat(errorObject).usingRecursiveComparison().ignoringFieldsOfTypes(Throwable.class).isEqualTo(AUDITABLE_WITH_ERROR)
                )
        );
    }
}
