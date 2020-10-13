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

package uk.gov.gchq.palisade.service.resource.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.RequestId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class GetResourcesByIdTest {

    private final GetResourcesByIdRequest expected = new GetResourcesByIdRequest();
    private final RequestId originalId = new RequestId().id("Original");

    @BeforeEach
    public void setup() {
        expected.setResourceId("Test");
        expected.setOriginalRequestId(originalId);
    }

    @Test
    public void testReturnRequestObject() {
        //Given
        GetResourcesByIdRequest actual = new GetResourcesByIdRequest();

        //When
        actual.resourceId("Test");

        //Then
        assertThat(expected.getResourceId()).isEqualTo(actual.getResourceId());
    }

    @Test
    public void testReturnErrorWithNoResourceId() {
        //Given
        GetResourcesByIdRequest actual = new GetResourcesByIdRequest();

        //When
        Exception nullPointerException = assertThrows(NullPointerException.class, () -> actual.setResourceId(null), "should throw nullPointerException");

        //Then an error is thrown
        assertThat((String) null).isEqualTo(nullPointerException.getMessage());
    }
}
