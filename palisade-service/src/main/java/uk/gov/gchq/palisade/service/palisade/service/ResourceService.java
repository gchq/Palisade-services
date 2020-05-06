/*
 * Copyright 2020 Crown Copyright
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.data.serialise.LineSerialiser;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.AbstractLeafResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private final ResourceClient client;
    private final Supplier<URI> uriSupplier;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    private final Serialiser<LeafResource> serialiser = new LineSerialiser<>() {
        @Override
        public LeafResource deserialiseLine(final String line) {
            try {
                return objectMapper.readValue(line, AbstractLeafResource.class);
            } catch (JsonProcessingException e) {
                LOGGER.error("Encountered JSONProccessingException while deserialising line {}", line);
                LOGGER.error("Exception was ", e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                LOGGER.error("Encountered IOException while deserialising line {}", line);
                LOGGER.error("Exception was ", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public String serialiseLine(final LeafResource obj) {
            LOGGER.error("No implementation of serialiseLine for {}, throwing exception", this);
            throw new NoSuchMethodError("No implementation of serialiseLine for " + this);
        }
    };


    public ResourceService(final ResourceClient resourceClient, final Supplier<URI> uriSupplier, final ObjectMapper objectMapper, final Executor executor) {
        this.client = resourceClient;
        this.uriSupplier = uriSupplier;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    public CompletableFuture<Set<LeafResource>> getResourcesById(final GetResourcesByIdRequest request) {
        LOGGER.info("Getting resources by id from resource service: {}", request);
        URI clientUri = this.uriSupplier.get();
        LOGGER.info("Using client uri: {}", clientUri);
        return CompletableFuture.supplyAsync(
                () -> getResourcesFromFeignResponse(() -> client.getResourcesById(clientUri, request)),
                this.executor);
    }

    public CompletableFuture<Set<LeafResource>> getResourcesByResource(final GetResourcesByResourceRequest request) {
        LOGGER.info("Getting resources by resource from resource service: {}", request);
        URI clientUri = this.uriSupplier.get();
        LOGGER.info("Using client uri: {}", clientUri);
        return CompletableFuture.supplyAsync(
                () -> getResourcesFromFeignResponse(() -> client.getResourcesByResource(clientUri, request)),
                this.executor);
    }

    public CompletableFuture<Set<LeafResource>> getResourcesByType(final GetResourcesByTypeRequest request) {
        LOGGER.info("Getting resources by type from resource service: {}", request);
        URI clientUri = this.uriSupplier.get();
        LOGGER.info("Using client uri: {}", clientUri);
        return CompletableFuture.supplyAsync(
                () -> getResourcesFromFeignResponse(() -> client.getResourcesByType(clientUri, request)),
                this.executor);
    }

    public CompletableFuture<Set<LeafResource>> getResourcesBySerialisedFormat(final GetResourcesBySerialisedFormatRequest request) {
        LOGGER.info("Getting resources from by serialised format resource service: {}", request);
        URI clientUri = this.uriSupplier.get();
        LOGGER.info("Using client uri: {}", clientUri);
        return CompletableFuture.supplyAsync(
                () -> getResourcesFromFeignResponse(() -> client.getResourcesBySerialisedFormat(clientUri, request)),
                this.executor);
    }

    protected Set<LeafResource> getResourcesFromFeignResponse(final Supplier<Response> feignCall) {
        try {
            InputStream responseStream = feignCall.get().body().asInputStream();
            Stream<LeafResource> resourceStream = serialiser.deserialise(responseStream);
            Set<LeafResource> response = resourceStream.collect(Collectors.toSet());
            LOGGER.info("Got resources: {}", response);
            return response;
        } catch (IOException e) {
            LOGGER.error("IOException getting response body input stream");
            LOGGER.error("Exception was ", e);
            throw new RuntimeException(e); //rethrow the exception
        }
    }

    public Response getHealth() {
        try {
            URI clientUri = this.uriSupplier.get();
            LOGGER.info("Using client uri: {}", clientUri);
            return this.client.getHealth(clientUri);
        } catch (Exception ex) {
            LOGGER.error("Failed to get health: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
    }
}
