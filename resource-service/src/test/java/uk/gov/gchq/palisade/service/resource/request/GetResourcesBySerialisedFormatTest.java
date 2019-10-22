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

@RunWith(JUnit4.class)
public class GetResourcesBySerialisedFormatTest {

    private final GetResourcesBySerialisedFormatRequest expected = new GetResourcesBySerialisedFormatRequest();
    private final RequestId originalId = new RequestId().id("Original");

    @Before
    public void setup() {
        expected.setSerialisedFormat("format");
        expected.setOriginalRequestId(originalId);
    }

    @Test(expected = NullPointerException.class)
    public void returnErrorWithNoSerialisedFormatTest() {
        GetResourcesBySerialisedFormatRequest actual = new GetResourcesBySerialisedFormatRequest();

        actual.setSerialisedFormat(null);
    }
}
