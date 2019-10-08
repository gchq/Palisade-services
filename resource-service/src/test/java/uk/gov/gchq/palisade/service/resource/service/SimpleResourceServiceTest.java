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

package uk.gov.gchq.palisade.service.resource.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class SimpleResourceServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceServiceTest.class);
    private ApplicationConfiguration config = new ApplicationConfiguration();
    private ResourceService service;
    private SimpleResourceService resourceService;

    @Before
    public void setup() throws Exception {
        resourceService = new SimpleResourceService(service, config.getAsyncExecutor());
    }

    @Test
    public void addResourceTest() throws Exception {
        try {
            resourceService.addResource(null);
            fail("exception expected");
        } catch (UnsupportedOperationException e) {
            assertEquals(SimpleResourceService.ERROR_ADD_RESOURCE, e.getMessage());
        }
    }
}
