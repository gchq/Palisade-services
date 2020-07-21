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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class ResultsResponseTest {

    @Autowired
    private JacksonTester<ResultsResponse> jacksonTester;

    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {
        ResultsResponse resultsResponse = ResultsResponse.Builder.create().withQueuePointer("testQueuePointer");
        JsonContent<ResultsResponse> resultsResponseJsonContent = jacksonTester.write(resultsResponse);

        assertThat(resultsResponseJsonContent).extractingJsonPathStringValue("$.queuePointer").isEqualTo("testQueuePointer");
    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {
        String jsonString = "{\"queuePointer\":\"testQueuePointer\"}";
        ObjectContent<ResultsResponse> resultsResponseObjectContent = jacksonTester.parse(jsonString);
        ResultsResponse queryScopeResponse = resultsResponseObjectContent.getObject();

        assertThat(queryScopeResponse.queuePointer).isEqualTo("testQueuePointer");
    }
}
