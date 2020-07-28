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
package uk.gov.gchq.palisade.service.data.request;

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
public class DataRequestTest {

    @Autowired
    private JacksonTester<DataRequest> jacksonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link DataRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    public void testGroupedDependantDataRequestSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        Rules rules = new Rules<>();

        DataRequest dataRequest = DataRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(context)
                .withUser(user)
                .withResource(resource)
                .withRules(rules);

        JsonContent<DataRequest> dataRequestJsonContent = jacksonTester.write(dataRequest);
        ObjectContent<DataRequest> dataRequestObjectContent = jacksonTester.parse(dataRequestJsonContent.getJson());
        DataRequest dataRequestMessageObject = dataRequestObjectContent.getObject();

        assertAll("AuditSerialisingDeseralisingAndComparison",
                () -> assertAll("AuditSerialisingComparedToString",
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("originalResourceID"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.resource.connectionDetail.serviceName").isEqualTo("test-service"),
                        () -> assertThat(dataRequestJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set")
                ),
                () -> assertAll("AuditDeserialisingComparedToObject",
                        () -> assertThat(dataRequestMessageObject.getUser()).isEqualTo(dataRequest.getUser()),
                        () -> assertThat(dataRequestMessageObject.getContext().getPurpose()).isEqualTo(dataRequest.getContext().getPurpose()),
                        () -> assertThat(dataRequestMessageObject.getContext()).isEqualTo(dataRequest.getContext()),
                        () -> assertThat(dataRequestMessageObject.getResource()).isEqualTo(dataRequest.getResource()),
                        () -> assertThat(dataRequestMessageObject.getRules()).isEqualTo(dataRequest.getRules())
                ),
                () -> assertAll("ObjectComparison",
                        //The reconstructed stack trace wont be exactly the same due to different object hashes so equals is used here
                        () -> assertThat(dataRequestMessageObject.equals(dataRequest))
                )
        );
    }
}
