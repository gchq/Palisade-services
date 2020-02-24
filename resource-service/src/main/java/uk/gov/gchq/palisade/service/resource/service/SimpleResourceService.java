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
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleResourceService implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceService.class);

    private Map<Resource, Map<LeafResource, ConnectionDetail>> resources = new HashMap<>();
    private Map<String, Resource> resourceIds = new HashMap<>();
    private Map<String, Resource> resourceTypes = new HashMap<>();
    private Map<String, Resource> resourceFormats = new HashMap<>();

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        LOGGER.debug("Getting resource by {}", resource);
        return resources.get(resource);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        LOGGER.debug("Getting resource by {}", resourceId);
        return resources.get(resourceIds.get(resourceId));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        LOGGER.debug("Getting resource by {}", resourceType);
        return resources.get(resourceTypes.get(resourceType));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceFormat) {
        LOGGER.debug("Getting resource by {}", resourceFormat);
        return resources.get(resourceFormats.get(resourceFormat));
    }

    @Override
    public Resource addResource(final Resource resource) {
        if (resource instanceof LeafResource) {
            LOGGER.info("Adding Resource [ {} ] to SimpleResourceService", resource);
            resources.put(resource, Collections.singletonMap((LeafResource) resource, new SimpleConnectionDetail().uri("localhost:8082")));
            LOGGER.info("Added to cache resources with key {}", resource);
            resourceIds.put(resource.getId(), resource);
            LOGGER.info("Added to cache resourceId with key {}", resource.getId());
            resourceTypes.put(((LeafResource) resource).getType(), resource);
            LOGGER.info("Added to cache resourceType with key {}", ((LeafResource) resource).getType());
            resourceFormats.put(((LeafResource) resource).getSerialisedFormat(), resource);
            LOGGER.info("Added to cache resourceFormat with key {}", ((LeafResource) resource).getSerialisedFormat());
            return resource;
        } else {
            return null;
        }
    }
}
