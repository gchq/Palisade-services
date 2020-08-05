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
package uk.gov.gchq.palisade.service.results.request;

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
public class ResultsRequestTest {

    @Autowired
    private JacksonTester<ResultsRequest> jsonTester;

    /**
     * Tests the creation of the message type, ResultsRequest using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testResultsRequestSerialisingAndDeserialising() throws IOException {

        Context context = new Context().purpose("testContext");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        ResultsRequest resultsRequest = ResultsRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withResource(resource);

        JsonContent<ResultsRequest> resultsRequestJsonContent = jsonTester.write(resultsRequest);
        ObjectContent<ResultsRequest> resultsRequestObjectContent = this.jsonTester.parse(resultsRequestJsonContent.getJson());
        ResultsRequest resultsRequestObject = resultsRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(resultsRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(resultsRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(resultsRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(resultsRequestJsonContent).extractingJsonPathStringValue("$.resource.connectionDetail.serviceName").isEqualTo("test-service")

                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(resultsRequest.getUserId()).isEqualTo(resultsRequestObject.getUserId()),
                        () -> assertThat(resultsRequest.getResourceId()).isEqualTo(resultsRequestObject.getResourceId()),
                        () -> assertThat(resultsRequest.getContext()).isEqualTo(resultsRequestObject.getContext()),
                        () -> assertThat(resultsRequest.getResource()).isEqualTo((resultsRequestObject.getResource()))
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(resultsRequest).isEqualTo(resultsRequestObject)
                )
        );
    }

}