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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;

public class StreamingResourceServiceProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingResourceServiceProxy.class);

    private final PersistenceLayer persistence;
    private final ResourceService delegate;
    private final Serialiser<LeafResource> serialiser;
    private Optional<ResourceConfiguration> resourceConfiguration;

    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final Serialiser<LeafResource> serialiser) {
        this.persistence = persistence;
        this.delegate = delegate;
        this.serialiser = serialiser;
        this.resourceConfiguration = Optional.empty();
    }

    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final Serialiser<LeafResource> serialiser, final ResourceConfiguration resourceConfiguration) {
        this(persistence, delegate, serialiser);
        this.resourceConfiguration = Optional.of(resourceConfiguration);
    }

    @Transactional
    public void getResourcesByResource(final Resource resource, final OutputStream outputStream) throws IOException {
        // Try first from persistence
        Stream<LeafResource> resourceStream = persistence.getResourcesById(resource.getId())
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesById(resource.getId())
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesById(resource.getId(), leafResource)));
        // Consume the stream, write to the output stream
        serialiser.serialise(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesById(final String resourceId, final OutputStream outputStream) throws IOException {
        // Try first from persistence
        Stream<LeafResource> resourceStream = persistence.getResourcesById(resourceId)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesById(resourceId)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesById(resourceId, leafResource)));
        // Consume the stream, write to the output stream
        serialiser.serialise(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesByType(final String type, final OutputStream outputStream) throws IOException {
        // Try first from persistence
        Stream<LeafResource> resourceStream = persistence.getResourcesByType(type)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesByType(type)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesByType(type, leafResource)));
        // Consume the stream, write to the output stream
        serialiser.serialise(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesBySerialisedFormat(final String serialisedFormat, final OutputStream outputStream) throws IOException {
        // Try first from persistence
        Stream<LeafResource> resourceStream = persistence.getResourcesByType(serialisedFormat)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesBySerialisedFormat(serialisedFormat)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesBySerialisedFormat(serialisedFormat, leafResource)));
        // Consume the stream, write to the output stream
        serialiser.serialise(resourceStream, outputStream);
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
        resourceConfiguration.ifPresent(config ->
                config.getResources().stream()
                        .map(ResourcePrepopulationFactory::build)
                        .forEach(leafResource -> {
                                persistence.putResourcesById(leafResource.getId(), leafResource);
                                persistence.putResourcesByType(leafResource.getType(), leafResource);
                                persistence.putResourcesBySerialisedFormat(leafResource.getSerialisedFormat(), leafResource);
                        }));
    }
}
