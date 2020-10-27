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

package uk.gov.gchq.palisade.service.resource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.data.serialise.LineSerialiser;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A proxy of (wrapper around) an instance of a {@link ResourceService}.
 * This adds a cache-like behaviour to the service by persisting requests/responses in a database.
 * Additionally, this is expected to be used by an asynchronous REST streaming response, so has further considerations
 * to properly support the callback.
 */
public class StreamingResourceServiceProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingResourceServiceProxy.class);
    private static final String STORE = "Trying from persistence store";
    private static final String EMPTY = "Persistence empty, delegating to resource-service";

    private final PersistenceLayer persistence;
    private final ResourceService delegate;
    private final ObjectMapper objectMapper;
    private Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder;

    private final Serialiser<LeafResource> serialiser = new LineSerialiser<>() {
        @Override
        public String serialiseLine(final LeafResource obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                LOGGER.error("Encountered JSONProccessingException while serialising object {}", obj);
                LOGGER.error("Exception was ", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public LeafResource deserialiseLine(final String line) {
            LOGGER.warn("No implementation of deserialiseLine, ignoring argument {}", line);
            return null;
        }
    };

    /**
     * Construct a StreamingResourceServiceProxy, but without any {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory} prepopulation
     *
     * @param persistence  a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate     a 'real' {@link ResourceService} to delegate requests to when not found in the persistence layer
     * @param objectMapper a {@link ObjectMapper} used for serialisation when writing each {@link Resource} to the {@link java.io.OutputStream}
     */

    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final ObjectMapper objectMapper) {
        this.persistence = persistence;
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.resourceBuilder = Collections::emptyList;
    }

    /**
     * Construct a StreamingResourceServiceProxy, passing each member as an argument
     *
     * @param persistence     a {@link PersistenceLayer} for persisting resources in, as if it were a cache
     * @param delegate        a 'real' {@link ResourceService} to delegate requests to when not found in the persistence layer
     * @param objectMapper    a {@link ObjectMapper} used for serialisation when writing each {@link Resource} to the {@link java.io.OutputStream}
     * @param resourceBuilder a {@link Supplier} of resources as built by a {@link uk.gov.gchq.palisade.service.ResourcePrepopulationFactory}, but with a connection detail attached
     */
    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final ObjectMapper objectMapper, final Supplier<List<Entry<Resource, LeafResource>>> resourceBuilder) {
        this(persistence, delegate, objectMapper);
        this.resourceBuilder = resourceBuilder;
    }

    @Transactional
    public CompletableFuture<LeafResource> getResourcesById(final ResourceRequest resourceRequest) {
        // Validate resourceId is a valid and normalised URI
        Resource normalisedResourceWithId = ResourceBuilder.create(resourceRequest.resourceId);
        String normalisedId = normalisedResourceWithId.getId();
        // Try first from persistence
        LOGGER.info(STORE);
        return CompletableFuture.completedFuture(persistence.withPersistenceById(normalisedId, delegate.getResourcesById(normalisedId)));
    }

    @Transactional
    public Boolean addResource(final LeafResource leafResource) {
        boolean success = delegate.addResource(leafResource);
        if (success) {
            persistence.addResource(leafResource);
        }
        return success;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        // Add resources to persistence
        LOGGER.info("Prepopulating using resource builder: {}", resourceBuilder);
        resourceBuilder.get()
                .forEach(entry -> {
                    Resource rootResource = entry.getKey();
                    LeafResource leafResource = entry.getValue();
                    LOGGER.info("Persistence add for {} -> {}", rootResource.getId(), leafResource.getId());
                    LeafResource resource = persistence.withPersistenceById(rootResource.getId(), leafResource);
                });
    }

    private void serialiseAndWriteStreamToOutput(final Stream<LeafResource> leafResourceStream, final OutputStream outputStream) {
        try {
            serialiser.serialise(leafResourceStream, outputStream);
        } catch (IOException e) {
            LOGGER.error("Encountered IOException while serialising line: ", e);
            throw new RuntimeException(e);
        }
    }
}
