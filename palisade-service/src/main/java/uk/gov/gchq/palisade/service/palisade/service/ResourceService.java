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

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private final ResourceClient client;
    private final Supplier<URI> uriSupplier;
    private final Serialiser<LeafResource> serialiser;
    private final Executor executor;

    public ResourceService(final ResourceClient resourceClient, final Supplier<URI> uriSupplier, final Serialiser<LeafResource> serialiser, final Executor executor) {
        this.client = resourceClient;
        this.uriSupplier = uriSupplier;
        this.serialiser = serialiser;
        this.executor = executor;
    }

    public CompletableFuture<Set<LeafResource>> getResourcesById(final GetResourcesByIdRequest request) {
        LOGGER.debug("Getting resources from resource service: {}", request);

        CompletionStage<Set<LeafResource>> resources;
        try {
            LOGGER.info("Resource request: {}", request);
            resources = CompletableFuture.supplyAsync(() -> {
                URI clientUri = this.uriSupplier.get();
                LOGGER.debug("Using client uri: {}", clientUri);
                InputStream responseStream = null;
                try {
                    responseStream = client.getResourcesById(clientUri, request).body().asInputStream();
                    Stream<LeafResource> resourceStream = serialiser.deserialise(responseStream);
                    Set<LeafResource> response = resourceStream.collect(Collectors.toSet());
                    LOGGER.info("Got resources: {}", response);
                    return response;
                } catch (IOException e) {
                    LOGGER.error("IOException getting response body input stream");
                    LOGGER.error("Exception was :: ", e);
                    throw new RuntimeException(e);
                }
            }, this.executor);
        } catch (Exception ex) {
            LOGGER.error("Failed to get resources: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }

        return resources.toCompletableFuture();
    }

    public Response getHealth() {
        try {
            URI clientUri = this.uriSupplier.get();
            LOGGER.debug("Using client uri: {}", clientUri);
            return this.client.getHealth(clientUri);
        } catch (Exception ex) {
            LOGGER.error("Failed to get health: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
    }
}
