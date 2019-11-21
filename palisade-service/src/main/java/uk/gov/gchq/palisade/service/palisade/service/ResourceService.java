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
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class ResourceService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private final ResourceClient client;
    private final Executor executor;

    public ResourceService(final ResourceClient resourceClient, final Executor executor) {
        this.client = resourceClient;
        this.executor = executor;
    }

    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(final GetResourcesByIdRequest resource) {
        LOGGER.debug("Getting resources from resource service: {}", resource);

        CompletionStage<Map<LeafResource, ConnectionDetail>> resources;
        try {
            resources = CompletableFuture.supplyAsync(() -> client.getResourcesById(resource));
            LOGGER.info("Got resources: {}", resources);
        } catch (Exception ex) {
            LOGGER.error("Failed to get resources: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
        return resources.toCompletableFuture();
    }
}
