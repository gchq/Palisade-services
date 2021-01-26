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

import uk.gov.gchq.palisade.service.data.model.DataRequestModel;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.DATA_REQUEST_MODEL;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.LEAF_RESOURCE_ID;
import static uk.gov.gchq.palisade.component.data.common.CommonTestData.TOKEN;

@JsonTest
@ContextConfiguration(classes = {DataRequestModelTest.class})
class DataRequestModelTest {

    @Autowired
    private JacksonTester<DataRequestModel> jacksonTester;

    /**
     * Creates the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws IOException throws if the {@link DataRequestModel} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantDataRequestSerialisingAndDeserialising() throws IOException {

        JsonContent<DataRequestModel> dataRequestJsonContent = jacksonTester.write(DATA_REQUEST_MODEL);
        ObjectContent<DataRequestModel> dataRequestObjectContent = jacksonTester.parse(dataRequestJsonContent.getJson());
        DataRequestModel dataRequestModelMessageObject = dataRequestObjectContent.getObject();

        assertAll("DataRequestSerialisingDeseralisingAndComparison",
                () -> assertAll("DataRequestSerialisingComparedToString",
                        () -> assertThat(dataRequestJsonContent)
                                .extractingJsonPathStringValue("$.token")
                                .isEqualTo(TOKEN),

                        () -> assertThat(dataRequestJsonContent)
                                .extractingJsonPathStringValue("$.leafResourceId")
                                .isEqualTo(LEAF_RESOURCE_ID)
                ),
                () -> assertAll("DataRequestDeserialisingComparedToObject",
                        () -> assertThat(dataRequestModelMessageObject.getToken())
                                .isEqualTo(TOKEN),

                        () -> assertThat(dataRequestModelMessageObject.getLeafResourceId())
                                .isEqualTo(LEAF_RESOURCE_ID)
                ),
                () -> assertAll("ObjectComparison",
                        () -> assertThat(dataRequestModelMessageObject).as("Check using equalTo").isEqualTo(DATA_REQUEST_MODEL),
                        () -> assertThat(dataRequestModelMessageObject).as("Check using recursion").usingRecursiveComparison().isEqualTo(DATA_REQUEST_MODEL)
                )
        );
    }
}
