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

public class GetResourcesBySerialisedFormatTest {

    private final GetResourcesBySerialisedFormatRequest expected = new GetResourcesBySerialisedFormatRequest();
    private final RequestId originalId = new RequestId().id("Original");

    @BeforeEach
    public void setup() {
        expected.setSerialisedFormat("format");
        expected.setOriginalRequestId(originalId);
    }

    @Test
    public void testReturnErrorWithNoSerialisedFormat() {
        GetResourcesBySerialisedFormatRequest actual = new GetResourcesBySerialisedFormatRequest();

        //When
        Exception nullPointerException = assertThrows(NullPointerException.class, () -> actual.setSerialisedFormat(null), "Null pointer should be thrown");

        //Then an error is thrown
        assertThat((String) null).isEqualTo(nullPointerException.getMessage());
    }
}
