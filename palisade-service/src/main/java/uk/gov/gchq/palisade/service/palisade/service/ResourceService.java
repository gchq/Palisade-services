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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ResourceService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private final ResourceClient client;
    private final Supplier<URI> uriSupplier;
    private final Executor executor;

    public ResourceService(final ResourceClient resourceClient, final Supplier<URI> uriSupplier, final Executor executor) {
        this.client = resourceClient;
        this.uriSupplier = uriSupplier;
        this.executor = executor;
    }

    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(final GetResourcesByIdRequest request) {
        LOGGER.debug("Getting resources from resource service: {}", request);

        CompletionStage<Map<LeafResource, ConnectionDetail>> resources;
        try {
            LOGGER.info("Resource request: {}", request);
            resources = CompletableFuture.supplyAsync(() -> {
                URI clientUri = this.uriSupplier.get();
                LOGGER.debug("Using client uri: {}", clientUri);
                Map<LeafResource, ConnectionDetail> response = client.getResourcesById(clientUri, request);
                LOGGER.info("Got resources: {}", response);
                return response;
            }, this.executor);
        } catch (Exception ex) {
            LOGGER.error("Failed to get resources: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }

        return resources.toCompletableFuture();
    }
}
