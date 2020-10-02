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
package uk.gov.gchq.palisade.component.attributemask.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {AttributeMaskingResponseTest.class})
class AttributeMaskingResponseTest {

    @Autowired
    private JacksonTester<AttributeMaskingResponse> jsonTester;

    @Test
    void testContextLoads() {
        assertThat(jsonTester).isNotNull();
    }

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AttributeMaskingResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantAttributeMaskingResponseSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        AttributeMaskingResponse attributeMaskingResponse = AttributeMaskingResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withResource(resource);

        JsonContent<AttributeMaskingResponse> attributeMaskingResponseJsonContent = jsonTester.write(attributeMaskingResponse);
        ObjectContent<AttributeMaskingResponse> attributeMaskingResponseObjectContent = jsonTester.parse(attributeMaskingResponseJsonContent.getJson());
        AttributeMaskingResponse attributeMaskingResponseMessageObject = attributeMaskingResponseObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(attributeMaskingResponseMessageObject.getUserId()).isEqualTo(attributeMaskingResponse.getUserId()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getContext()).isEqualTo(attributeMaskingResponse.getContext()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getResource()).isEqualTo(attributeMaskingResponse.getResource()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getResourceId()).isEqualTo(attributeMaskingResponse.getResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(attributeMaskingResponseMessageObject.equals(attributeMaskingResponse))
                )
        );
    }
}
