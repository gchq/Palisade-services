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
package uk.gov.gchq.palisade.service.queryscope.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RunWith(SpringRunner.class)
@JsonTest
public class QueryScopeResponseTest {

    @Autowired
    private JacksonTester<QueryScopeResponse> jsonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        QueryScopeResponse queryScopeResponse = QueryScopeResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withResource(resource);

        JsonContent<QueryScopeResponse> queryScopeResponseJsonContent = jsonTester.write(queryScopeResponse);

        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID");
        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format");
    }

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link QueryScopeResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void groupedDependantQueryScopeResponseSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        QueryScopeResponse queryScopeResponse = QueryScopeResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withResource(resource);

        JsonContent<QueryScopeResponse> queryScopeResponseJsonContent = jsonTester.write(queryScopeResponse);
        ObjectContent<QueryScopeResponse> queryScopeResponseObjectContentObjectContent = jsonTester.parse(queryScopeResponseJsonContent.getJson());
        QueryScopeResponse queryScopeResponseMessageObject = queryScopeResponseObjectContentObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(queryScopeResponseMessageObject.getUserId()).isEqualTo(queryScopeResponse.getUserId()),
                        () -> assertThat(queryScopeResponseMessageObject.getContext()).isEqualTo(queryScopeResponse.getContext()),
                        () -> assertThat(queryScopeResponseMessageObject.getResource()).isEqualTo(queryScopeResponse.getResource()),
                        () -> assertThat(queryScopeResponseMessageObject.getResourceId()).isEqualTo(queryScopeResponse.getResourceId())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(queryScopeResponseMessageObject.equals(queryScopeResponse))
                )
        );
    }
}
