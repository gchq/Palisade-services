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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.Resource;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class GetResourcesByResourceTest {

    private final GetResourcesByResourceRequest expected = new GetResourcesByResourceRequest();
    private final RequestId originalId = new RequestId().id("Original");
    @Mock
    private Resource mockResource;

    @Before
    public void setup() {
        expected.setResource(mockResource);
        expected.setOriginalRequestId(originalId);
    }

    @Test
    public void returnRequestObjectTest() {
        // Given
        GetResourcesByResourceRequest actual = new GetResourcesByResourceRequest();

        // When
        actual.resource(mockResource);

        // Then
        assertEquals(expected.getResource(), actual.getResource());
    }

    @Test(expected = NullPointerException.class)
    public void returnErrorWithNoResourceIdTest() {
        //Given
        GetResourcesByResourceRequest actual = new GetResourcesByResourceRequest();

        //When
        actual.setResource(null);
    }
}
