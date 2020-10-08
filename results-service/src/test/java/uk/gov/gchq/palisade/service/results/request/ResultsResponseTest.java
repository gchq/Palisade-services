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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class ResultsResponseTest {

    @Autowired
    private JacksonTester<ResultsResponse> jsonTester;

    /**
     * Tests the creation of the message type, ResultsResponse using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {
        Long x = 31415L;
        ResultsResponse resultsResponse = ResultsResponse.Builder.create()
                .withQueuePointer(x);

        JsonContent<ResultsResponse> resultsResponseJsonContent = jsonTester.write(resultsResponse);
        ObjectContent<ResultsResponse> resultsResponseObjectContent = this.jsonTester.parse(resultsResponseJsonContent.getJson());
        ResultsResponse resultsResponseObject = resultsResponseObjectContent.getObject();

        assertThat(resultsResponseJsonContent).extractingJsonPathNumberValue("$.queuePointer").isEqualTo(x.intValue());
        assertThat(resultsResponse.queuePointer).isEqualTo(resultsResponseObject.queuePointer);
        assertThat(resultsResponse).isEqualTo(resultsResponseObject);
    }
}
