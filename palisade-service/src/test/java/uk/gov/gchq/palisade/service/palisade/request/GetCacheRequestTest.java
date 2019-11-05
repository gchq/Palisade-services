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

package uk.gov.gchq.palisade.service.palisade.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.exception.ForbiddenException;

@RunWith(JUnit4.class)
public class GetCacheRequestTest {

    private GetCacheRequest cacheRequest = new GetCacheRequest();

    @Test(expected = ForbiddenException.class)
    public void setOriginalRequestId() {
        cacheRequest.setOriginalRequestId(new RequestId().id("Test"));
    }

    @Test(expected = ForbiddenException.class)
    public void getOriginalRequestId() {
        RequestId id = cacheRequest.getOriginalRequestId();
    }
}
