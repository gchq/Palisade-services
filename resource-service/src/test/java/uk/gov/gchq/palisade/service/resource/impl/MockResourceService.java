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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class MockResourceService extends HashMap<String, Resource> implements ResourceService {
    private Map<LeafResource, ConnectionDetail> resourcesForIds = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForResource = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForType = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForSerialisedFormat = Mockito.mock(Map.class);

    private Map<Class<? extends Request>, Map<LeafResource, ConnectionDetail>> resourceMap = new HashMap<>();
    {
        resourceMap.put(GetResourcesByIdRequest.class, resourcesForIds);
        resourceMap.put(GetResourcesByTypeRequest.class, resourcesForType);
        resourceMap.put(GetResourcesBySerialisedFormatRequest.class, resourcesForSerialisedFormat);
        resourceMap.put(GetResourcesByResourceRequest.class, resourcesForResource);
    }

    public Map<Class<? extends Request>, Map<LeafResource, ConnectionDetail>> getMockingMap() {
        return resourceMap;
    }

    private Function<Request, Supplier<Map<LeafResource, ConnectionDetail>>> resourceSupplier;
    private Exception serviceThrows;

    public MockResourceService() {
        resourceSupplier = request -> () -> resourceMap.get(request.getClass());
    }

    public MockResourceService(Exception ex) {
        super();
        this.willThrow(ex);
    }

    public void willThrow(Exception ex) {
        serviceThrows = ex;
    }

    private Map<LeafResource, ConnectionDetail> handleRequest(Request request) {
        return resourceSupplier.apply(request).get();
    }
    private Resource handleAddRequest(Resource resource) {
        return resource;
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        GetResourcesByResourceRequest resourceRequest = new GetResourcesByResourceRequest();
        return handleRequest(resourceRequest);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        GetResourcesByIdRequest idRequest = new GetResourcesByIdRequest();
        return handleRequest(idRequest);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        GetResourcesByTypeRequest typeRequest = new GetResourcesByTypeRequest();
        return handleRequest(typeRequest);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceFormat) {
        GetResourcesBySerialisedFormatRequest formatRequest = new GetResourcesBySerialisedFormatRequest();
        return handleRequest(formatRequest);
    }

    @Override
    public Resource addResource(final Resource resource) {
        return handleAddRequest(resource);
    }
}