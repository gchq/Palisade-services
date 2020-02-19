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

import java.util.Map;

public class NullResourceService implements ResourceService {


    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        throw new RuntimeException(String.format("No resource matching %s found in cache", resource));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        throw new RuntimeException(String.format("No resource matching id %s found in cache", resourceId));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        throw new RuntimeException(String.format("No resource matching type %s found in cache", resourceType));
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceFormat) {
        throw new RuntimeException(String.format("No resource matching format %s found in cache", resourceFormat));
    }

    @Override
    public Resource addResource(final Resource leafResource) {
        return leafResource;
    }
}
