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
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;

public class StreamingResourceServiceProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingResourceServiceProxy.class);

    private final PersistenceLayer persistence;
    private final ResourceService delegate;
    private final ObjectMapper objectMapper;
    private Optional<ResourceConfiguration> resourceConfiguration;

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

    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final ObjectMapper objectMapper) {
        this.persistence = persistence;
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.resourceConfiguration = Optional.empty();
    }

    public StreamingResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate, final ObjectMapper objectMapper, final ResourceConfiguration resourceConfiguration) {
        this(persistence, delegate, objectMapper);
        this.resourceConfiguration = Optional.of(resourceConfiguration);
    }

    @Transactional
    public void getResourcesByResource(final Resource resource, final OutputStream outputStream) {
        // Try first from persistence
        LOGGER.info("Trying from persistence store");
        Stream<LeafResource> resourceStream = persistence.getResourcesById(resource.getId())
                // Otherwise call out to resource service
                .orElseGet(() -> {
                    LOGGER.info("Persistence empty, delegating to resource-service");
                    // Persist returned resources as the stream is consumed
                    return persistence.withPersistenceById(resource.getId(), delegate.getResourcesById(resource.getId()));
                });
        // Consume the stream, write to the output stream
        serialiseAndWriteStreamToOutput(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesById(final String resourceId, final OutputStream outputStream) {
        // Try first from persistence
        LOGGER.info("Trying from persistence store");
        Stream<LeafResource> resourceStream = persistence.getResourcesById(resourceId)
                // Otherwise call out to resource service
                .orElseGet(() -> {
                    LOGGER.info("Persistence empty, delegating to resource-service");
                    // Persist returned resources as the stream is consumed
                    return persistence.withPersistenceById(resourceId, delegate.getResourcesById(resourceId));
                });
        // Consume the stream, write to the output stream
        serialiseAndWriteStreamToOutput(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesByType(final String type, final OutputStream outputStream) {
        // Try first from persistence
        LOGGER.info("Trying from persistence store");
        Stream<LeafResource> resourceStream = persistence.getResourcesByType(type)
                // Otherwise call out to resource service
                .orElseGet(() -> {
                    LOGGER.info("Persistence empty, delegating to resource-service");
                    // Persist returned resources as the stream is consumed
                    return persistence.withPersistenceByType(type, delegate.getResourcesByType(type));
                });
        // Consume the stream, write to the output stream
        serialiseAndWriteStreamToOutput(resourceStream, outputStream);
    }

    @Transactional
    public void getResourcesBySerialisedFormat(final String serialisedFormat, final OutputStream outputStream) {
        // Try first from persistence
        LOGGER.debug("Trying from persistence store");
        Stream<LeafResource> resourceStream = persistence.getResourcesBySerialisedFormat(serialisedFormat)
                // Otherwise call out to resource service
                .orElseGet(() -> {
                    LOGGER.debug("Persistence empty, delegating to resource-service");
                    // Persist returned resources as the stream is consumed
                    return persistence.withPersistenceBySerialisedFormat(serialisedFormat, delegate.getResourcesBySerialisedFormat(serialisedFormat));
                });
        // Consume the stream, write to the output stream
        serialiseAndWriteStreamToOutput(resourceStream, outputStream);
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
                config.getResources().forEach((rootResourceId, prepopulationFactoryList) -> {
                            Resource rootResource = ResourceBuilder.create(rootResourceId);
                            prepopulationFactoryList.stream()
                                .map(ResourcePrepopulationFactory::build)
                                .forEach(leafResource -> {
                                    Stream<LeafResource> resources = Stream.of(leafResource);
                                    resources = persistence.withPersistenceById(rootResource.getId(), resources);
                                    resources = persistence.withPersistenceByType(leafResource.getType(), resources);
                                    resources = persistence.withPersistenceBySerialisedFormat(leafResource.getSerialisedFormat(), resources);
                                    resources.forEach(x -> { });
                                });
                        }));
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
