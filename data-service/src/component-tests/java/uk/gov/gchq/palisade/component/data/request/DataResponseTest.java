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
package uk.gov.gchq.palisade.component.data.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.request.DataResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JsonTest
@ContextConfiguration(classes = {DataResponseTest.class})
class DataResponseTest {

    @Autowired
    private JacksonTester<DataResponse> jacksonTester;

    /**
     * Grouped assertion test
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link DataResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantDataResponseSerialisingAndDeserialising() throws IOException {
        Context context = new Context().purpose("testContext");
        User user = new User().userId("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        Rules<?> rules = new Rules<>();

        DataResponse dataResponse = DataResponse.Builder.create()
                .withContext(context)
                .withUser(user)
                .withResource(resource)
                .withRules(rules);

        JsonContent<DataResponse> dataResponseJsonContent = jacksonTester.write(dataResponse);
        ObjectContent<DataResponse> dataRequestObjectContent = jacksonTester.parse(dataResponseJsonContent.getJson());
        DataResponse dataResponseMessageObject = dataRequestObjectContent.getObject();

        assertAll("DataResponseSerialisingDeseralisingAndComparison",
                () -> assertAll("DataResponseSerialisingComparedToString",
                        () -> assertThat(dataResponseJsonContent).extractingJsonPathStringValue("$.user.userId.id").isEqualTo("testUserId"),
                        () -> assertThat(dataResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext"),
                        () -> assertThat(dataResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format"),
                        () -> assertThat(dataResponseJsonContent).extractingJsonPathStringValue("$.resource.connectionDetail.serviceName").isEqualTo("test-service"),
                        () -> assertThat(dataResponseJsonContent).extractingJsonPathStringValue("$.rules.message").isEqualTo("no rules set")
                ),
                () -> assertAll("DataResponseDeserialisingComparedToObject",
                        () -> assertThat(dataResponseMessageObject.getUser()).isEqualTo(dataResponse.getUser()),
                        () -> assertThat(dataResponseMessageObject.getContext().getPurpose()).isEqualTo(dataResponse.getContext().getPurpose()),
                        () -> assertThat(dataResponseMessageObject.getContext()).isEqualTo(dataResponse.getContext()),
                        () -> assertThat(dataResponseMessageObject.getResource()).isEqualTo(dataResponse.getResource()),
                        () -> assertThat(dataResponseMessageObject.getRules()).isEqualTo(dataResponse.getRules())
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(dataResponseMessageObject).usingRecursiveComparison().isEqualTo(dataResponse)
                )
        );
    }
}
