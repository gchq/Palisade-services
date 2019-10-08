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
package uk.gov.gchq.palisade.service.resource.impl;

import org.mockito.Mockito;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;

public class MockResourceService {

    private static SimpleResourceService mock = Mockito.mock(SimpleResourceService.class);

    public static SimpleResourceService getMock() {
        return mock;
    }

    public static void setMock(final SimpleResourceService mock) {
        if (mock == null) {
            MockResourceService.mock = Mockito.mock(SimpleResourceService.class);
        }
        MockResourceService.mock = mock;
    }
}
