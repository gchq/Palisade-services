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
package uk.gov.gchq.palisade.component.palisade.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.palisade.request.OriginalRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = OriginalRequestTest.class)
class OriginalRequestTest {

    @Autowired
    private JacksonTester<OriginalRequest> jsonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link OriginalRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantErrorMessageSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        OriginalRequest originalRequest = OriginalRequest.Builder.create()
                .withUserId("testUser")
                .withResourceId("testResource")
                .withContext(context);

        JsonContent<OriginalRequest> originalRequestJsonContent = jsonTester.write(originalRequest);
        ObjectContent<OriginalRequest> originalRequestObjectContent = jsonTester.parse(originalRequestJsonContent.getJson());
        OriginalRequest originalRequestMessageObject = originalRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUser"),
                        () -> assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResource"),
                        () -> assertThat(originalRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(originalRequestMessageObject.getUserId()).isEqualTo(originalRequest.getUserId()),
                        () -> assertThat(originalRequestMessageObject.getResourceId()).isEqualTo(originalRequest.getResourceId()),
                        () -> assertThat(originalRequestMessageObject.getContext().getPurpose()).isEqualTo(originalRequest.getContext().getPurpose())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(originalRequestMessageObject).usingRecursiveComparison().isEqualTo(originalRequest)
                )
        );
    }
}