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
import uk.gov.gchq.palisade.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class MockResourceService implements ResourceService {
    private Map<LeafResource, ConnectionDetail> resourcesForIds = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForResource = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForType = Mockito.mock(Map.class);
    private Map<LeafResource, ConnectionDetail> resourcesForSerialisedFormat = Mockito.mock(Map.class);

    private Map<Class<? extends Request>, Map<LeafResource, ConnectionDetail>> resourceMap = new HashMap<>();
    private Function<Request, Supplier<Map<LeafResource, ConnectionDetail>>> resourceSupplier;
    private Exception serviceThrows;

    {
        resourceMap.put(GetResourcesByIdRequest.class, resourcesForIds);
        resourceMap.put(GetResourcesByTypeRequest.class, resourcesForType);
        resourceMap.put(GetResourcesBySerialisedFormatRequest.class, resourcesForSerialisedFormat);
        resourceMap.put(GetResourcesByResourceRequest.class, resourcesForResource);
    }
    public MockResourceService() {
        resourceSupplier = request -> () -> resourceMap.get(request.getClass());
    }

    public MockResourceService(final Exception ex) {
        super();
        this.willThrow(ex);
    }

    public Map<Class<? extends Request>, Map<LeafResource, ConnectionDetail>> getMockingMap() {
        return resourceMap;
    }

    public void willThrow(final Exception ex) {
        serviceThrows = ex;
    }

    private CompletableFuture<Map<LeafResource, ConnectionDetail>> handleRequest(final Request request) {
        CompletableFuture<Map<LeafResource, ConnectionDetail>> future = CompletableFuture.supplyAsync(resourceSupplier.apply(request));
        if (serviceThrows != null) {
            future.obtrudeException(serviceThrows);
        }
        return future;
    }

    private CompletableFuture<Boolean> handleRequest(final AddResourceRequest request) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> true);
        if (serviceThrows != null) {
            future.obtrudeException(serviceThrows);
        }
        return future;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByResource(final GetResourcesByResourceRequest request) {
        return handleRequest(request);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(final GetResourcesByIdRequest request) {
        return handleRequest(request);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByType(final GetResourcesByTypeRequest request) {
        return handleRequest(request);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesBySerialisedFormat(final GetResourcesBySerialisedFormatRequest request) {
        return handleRequest(request);
    }

    @Override
    public CompletableFuture<Boolean> addResource(final AddResourceRequest request) {
        return handleRequest(request);
    }
}