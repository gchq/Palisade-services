/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.resource.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.gchq.palisade.RequestId;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class GetResourcesByIdTest {

    private final GetResourcesByIdRequest expected = new GetResourcesByIdRequest();
    private final RequestId originalId = new RequestId().id("Original");

    @Before
    public void setup() {
        expected.setResourceId("Test");
        expected.setOriginalRequestId(originalId);
    }

    @Test
    public void returnRequestObjectTest() {
            //Given
            GetResourcesByIdRequest actual = new GetResourcesByIdRequest();

            //When
            actual.resourceId("Test");

            //Then
            assertEquals(expected.getResourceId(), actual.getResourceId());
    }

    @Test (expected = NullPointerException.class)
    public void returnErrorWithNoResourceIdTest() {
        //Given
        GetResourcesByIdRequest actual = new GetResourcesByIdRequest();

        //When
        actual.setResourceId(null);
    }
}
