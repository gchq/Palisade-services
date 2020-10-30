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

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
    public Source<LeafResource, NotUsed> getResourcesByResource(final Resource resource) {
        // Try first from persistence
        LOGGER.info(STORE);
        return Source.fromIterator(() -> {
            Iterator<LeafResource> persistenceIterator = persistence.getResourcesById(resource.getId());
            if (persistenceIterator.hasNext()) {
                return persistenceIterator;
            } else {
                LOGGER.info(EMPTY);
                return persistence.withPersistenceById(resource.getId(), delegate.getResourcesById(resource.getId()));
            }
        });
    }

    @Transactional
    public Source<LeafResource, NotUsed> getResourcesById(final String resourceId) {
        // Try first from persistence
        LOGGER.info(STORE);
        return Source.fromIterator(() -> {
            Iterator<LeafResource> persistenceIterator = persistence.getResourcesById(resourceId);
            if (persistenceIterator.hasNext()) {
                return persistenceIterator;
            } else {
                LOGGER.info(EMPTY);
                return persistence.withPersistenceById(resourceId, delegate.getResourcesById(resourceId));
            }
        });
    }

    @Transactional
    public Source<LeafResource, NotUsed> getResourcesByType(final String type) {
        // Try first from persistence
        LOGGER.info(STORE);
        return Source.fromIterator(() -> {
            Iterator<LeafResource> persistenceIterator = persistence.getResourcesByType(type);
            if (persistenceIterator.hasNext()) {
                return persistenceIterator;
            } else {
                LOGGER.info(EMPTY);
                return persistence.withPersistenceByType(type, delegate.getResourcesById(type));
            }
        });
    }

    @Transactional
    public Source<LeafResource, NotUsed> getResourcesBySerialisedFormat(final String serialisedFormat) {
        // Try first from persistence
        LOGGER.debug(STORE);
        return Source.fromIterator(() -> {
            Iterator<LeafResource> persistenceIterator = persistence.getResourcesBySerialisedFormat(serialisedFormat);
            if (persistenceIterator.hasNext()) {
                return persistenceIterator;
            } else {
                LOGGER.info(EMPTY);
                return persistence.withPersistenceBySerialisedFormat(serialisedFormat, delegate.getResourcesById(serialisedFormat));
            }
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

    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        // Add resources to persistence
        LOGGER.info("Prepopulating using resource builder: {}", resourceBuilder);
        resourceBuilder.get()
                .forEach(entry -> {
                    Resource rootResource = entry.getKey();
                    LeafResource leafResource = entry.getValue();
                    List<LeafResource> leafResourceList = new ArrayList<>();
                    leafResourceList.add(leafResource);
                    LOGGER.info("Persistence add for {} -> {}", rootResource.getId(), leafResource.getId());
                    Iterator<LeafResource> resourceIterator = leafResourceList.iterator();
                    resourceIterator = persistence.withPersistenceById(rootResource.getId(), resourceIterator);
                    resourceIterator = persistence.withPersistenceByType(leafResource.getType(), resourceIterator);
                    resourceIterator = persistence.withPersistenceBySerialisedFormat(leafResource.getSerialisedFormat(), resourceIterator);
                    while (resourceIterator.hasNext()) {
                        LeafResource resource = resourceIterator.next();
                        LOGGER.debug("Resource {} persisted", resource.getId());
                    }
                });
    }
}
