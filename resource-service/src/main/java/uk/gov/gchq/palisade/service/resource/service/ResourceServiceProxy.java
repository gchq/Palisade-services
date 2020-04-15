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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;

import java.util.stream.Stream;

public class ResourceServiceProxy implements ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceProxy.class);

    private PersistenceLayer persistence;
    private ResourceService delegate;

    public ResourceServiceProxy(final PersistenceLayer persistence, final ResourceService delegate) {
        this.persistence = persistence;
        this.delegate = delegate;
    }

    @Override
    public Stream<LeafResource> getResourcesByResource(final Resource resource) {
        return getResourcesById(resource.getId());
    }

    @Override
    public Stream<LeafResource> getResourcesById(final String resourceId) {
        // Try first from persistence
        return persistence.getResourcesById(resourceId)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesById(resourceId)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesById(resourceId, leafResource)));
    }

    @Override
    public Stream<LeafResource> getResourcesByType(final String type) {
        // Try first from persistence
        return persistence.getResourcesByType(type)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesByType(type)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesByType(type, leafResource)));
    }

    @Override
    public Stream<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        // Try first from persistence
        return persistence.getResourcesByType(serialisedFormat)
                // Otherwise call out to resource service
                .orElseGet(() -> delegate.getResourcesBySerialisedFormat(serialisedFormat)
                        // Persist the new resources
                        .peek(leafResource -> persistence.putResourcesBySerialisedFormat(serialisedFormat, leafResource)));
    }

    @Override
    public Boolean addResource(final LeafResource leafResource) {
        boolean success = delegate.addResource(leafResource);
        if (success) {
            persistence.addResource(leafResource);
        }
        return success;
    }
}
