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
package uk.gov.gchq.palisade.service.attributemask.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class AttributeMaskingRequestTest {
    @Autowired
    private JacksonTester<AttributeMaskingRequest> jacksonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link AttributeMaskingRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void testGroupedDependantQueryScopeRequestSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        Rules rules = new Rules<>();

        AttributeMaskingRequest attributeMaskingRequest = AttributeMaskingRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource)
                .withRules(rules);

        JsonContent<AttributeMaskingRequest> attributeMaskingResponseJsonContent = jacksonTester.write(attributeMaskingRequest);
        ObjectContent<AttributeMaskingRequest> attributeMaskingResponseObjectContent = jacksonTester.parse(attributeMaskingResponseJsonContent.getJson());
        AttributeMaskingRequest attributeMaskingResponseMessageObject = attributeMaskingResponseObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format"),
                        () -> assertThat(attributeMaskingResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(attributeMaskingResponseMessageObject.getUser()).isEqualTo(attributeMaskingRequest.getUser()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getContext()).isEqualTo(attributeMaskingRequest.getContext()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getResource()).isEqualTo(attributeMaskingRequest.getResource()),
                        () -> assertThat(attributeMaskingResponseMessageObject.getRules()).isEqualTo(attributeMaskingRequest.getRules())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(attributeMaskingResponseMessageObject.equals(attributeMaskingRequest))
                )
        );
    }
}
