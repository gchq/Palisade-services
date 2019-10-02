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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.resource.request.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ResourceService implements IResourceService {

    public ResourceService() {

    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByResource(GetResourcesByResourceRequest request) {
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(GetResourcesByIdRequest request) {
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByType(GetResourcesByTypeRequest request) {
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesBySerialisedFormat(GetResourcesBySerialisedFormatRequest request) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> addResource(AddResourceRequest request) {
        return null;
    }
}
