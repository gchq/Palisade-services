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

package uk.gov.gchq.palisade.service.palisade.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.impl.MockDataService;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ResourceServiceTest {

    private ResourceClient resourceClient = Mockito.mock(ResourceClient.class);
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private ResourceService resourceService;
    private Map<LeafResource, ConnectionDetail> resources = new HashMap<>();

    @Before
    public void setup() {
        resourceService = new ResourceService(resourceClient, applicationConfig.getAsyncExecutor());
        FileResource resource = new FileResource().id("/path/to/bob_file.txt");
        ConnectionDetail connectionDetail = new SimpleConnectionDetail().service(new MockDataService());
        resources.put(resource, connectionDetail);
    }

    @Test
    public void getResourceByIdReturnsMappedResources() {
        //Given
        when(resourceClient.getResourcesById(any(GetResourcesByIdRequest.class))).thenReturn(resources);

        //When
        GetResourcesByIdRequest request = new GetResourcesByIdRequest().resourceId("/path/to/bob_file.txt");
        Map<LeafResource, ConnectionDetail> actual = resourceService.getResourcesById(request);

        //Then
        assertEquals(resources, actual);
    }

}
