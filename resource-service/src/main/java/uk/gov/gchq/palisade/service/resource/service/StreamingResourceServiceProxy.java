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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;

import java.util.Iterator;

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
    }

    /**
     * Uses a resource to get any {@link LeafResource}s associated with the it.
     *
     * @param resource  the resource to be queried
     * @return          a {@link FunctionalIterator} of {@link LeafResource}s associated with the resource
     */
    @Transactional
    public Iterator<LeafResource> getResourcesByResource(final Resource resource) {
        // Try first from persistence
        LOGGER.info(STORE);
        return persistence.getResourcesById(resource.getId())
                .orElseGet(() -> {
                    LOGGER.info(EMPTY);
                    return persistence.withPersistenceById(resource.getId(),
                            FunctionalIterator.fromIterator(delegate.getResourcesById(resource.getId())));
                });
    }

    /**
     * Uses a resourceId to get any {@link LeafResource}s associated with the it.
     *
     * @param resourceId    the resourceId to be queried
     * @return              a {@link FunctionalIterator} of {@link LeafResource}s associated with the resourceId
     */
    @Transactional
    public Iterator<LeafResource> getResourcesById(final String resourceId) {
        // Try first from persistence
        LOGGER.info(STORE);
        return persistence.getResourcesById(resourceId)
                .orElseGet(() -> {
                    LOGGER.info(EMPTY);
                    return persistence.withPersistenceById(resourceId,
                            FunctionalIterator.fromIterator(delegate.getResourcesById(resourceId)));
                });
    }

    /**
     * Uses a type to get any {@link LeafResource}s associated with the it.
     *
     * @param type  the type to be queried
     * @return      a {@link FunctionalIterator} of {@link LeafResource}s associated with the type
     */
    @Transactional
    public Iterator<LeafResource> getResourcesByType(final String type) {
        // Try first from persistence
        LOGGER.info(STORE);
        return persistence.getResourcesByType(type)
                .orElseGet(() -> {
                    LOGGER.info(EMPTY);
                    return persistence.withPersistenceByType(type,
                            FunctionalIterator.fromIterator(delegate.getResourcesById(type)));
                });
    }

    /**
     * Uses a serialisedFormat to get any {@link LeafResource}s associated with the it.
     *
     * @param serialisedFormat  the serialisedFormat to be queried
     * @return                  a {@link FunctionalIterator} of {@link LeafResource}s associated with the type
     */
    @Transactional
    public Iterator<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        // Try first from persistence
        LOGGER.debug(STORE);
        return persistence.getResourcesBySerialisedFormat(serialisedFormat)
                .orElseGet(() -> {
                    LOGGER.info(EMPTY);
                    return persistence.withPersistenceBySerialisedFormat(serialisedFormat,
                            FunctionalIterator.fromIterator(delegate.getResourcesById(serialisedFormat)));
                });

    }

    @Transactional
    public Boolean addResource(final LeafResource leafResource) {
        boolean success = delegate.addResource(leafResource);
        if (success) {
            persistence.addResource(leafResource);
        }
        return success;
    }
}
