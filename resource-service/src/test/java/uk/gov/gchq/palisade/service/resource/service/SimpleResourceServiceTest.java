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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByIdRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleResourceServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceServiceTest.class);
    private static final String TEST_RESOURCE_ID = "/home/user/other/thing_file.json";
    private static final String TEST_SERIALISED_FORMAT = "json";
    private static final String TEST_DATA_TYPE = "thing";
    private static final String TEST_CONNECTION_CLASS = "class of the connection";

    private ApplicationConfiguration config = new ApplicationConfiguration();
    private ResourceService service;
    private SimpleResourceService resourceService;
    private GetResourcesByIdRequest idRequest = new GetResourcesByIdRequest();

    @Before
    public void setup() throws Exception {
        resourceService = new SimpleResourceService(service, config.getAsyncExecutor());
    }

    @Test
    public void getResourcesByIdTest() throws Exception {

        //Given
        CompletableFuture<Map<LeafResource, ConnectionDetail>> expected = mockCompletableFuture();

        RequestId originalId = new RequestId().id("original");
        idRequest.setResourceId("data");
        idRequest.setOriginalRequestId(originalId);

        //When
        CompletableFuture<Map<LeafResource, ConnectionDetail>> actual = resourceService.getResourcesById(idRequest);

        //Then
        assertNull(actual);
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

    private LeafResource mockResource() {
        final LeafResource mockResource = Mockito.mock(LeafResource.class);
        Mockito.doReturn(TEST_RESOURCE_ID).when(mockResource).getId();
        Mockito.doReturn(TEST_DATA_TYPE).when(mockResource).getType();
        Mockito.doReturn(TEST_SERIALISED_FORMAT).when(mockResource).getSerialisedFormat();
        return mockResource;
    }

    private ConnectionDetail mockConnection() {
        final ConnectionDetail mockConnection = Mockito.mock(ConnectionDetail.class);
        Mockito.doReturn(TEST_CONNECTION_CLASS).when(mockConnection)._getClass();
        return mockConnection;
    }

    private CompletableFuture<Map<LeafResource, ConnectionDetail>> mockCompletableFuture() {

        final CompletableFuture<Map<LeafResource, ConnectionDetail>> future = new CompletableFuture<Map<LeafResource, ConnectionDetail>>();
        final Map<LeafResource, ConnectionDetail> map = new HashMap<>();
        map.put(mockResource(), mockConnection());
        future.complete(map);
        return future;
    }
}
