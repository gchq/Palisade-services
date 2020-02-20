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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleResourceService implements ResourceService {

    private Map<Resource, Map<LeafResource, ConnectionDetail>> resources = new HashMap<>();
    private Map<String, Resource> resourceIds = new HashMap<>();
    private Map<String, Resource> resourceTypes = new HashMap<>();
    private Map<String, Resource> resourceFormats = new HashMap<>();

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        return resources.get(resource);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        return resources.get(resourceIds.get(resourceId));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        return resources.get(resourceTypes.get(resourceType));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceFormat) {
        return resources.get(resourceFormats.get(resourceFormat));
    }

    @Override
    public Resource addResource(final Resource resource) {
        if (resource instanceof LeafResource) {
            resources.put(resource, Collections.singletonMap((LeafResource) resource, new SimpleConnectionDetail().uri("localhost:8082")));
            resourceIds.put(resource.getId(), resource);
            resourceTypes.put(((LeafResource) resource).getType(), resource);
            resourceFormats.put(((LeafResource) resource).getSerialisedFormat(), resource);
            return resource;
        } else {
            return null;
        }
    }
}
