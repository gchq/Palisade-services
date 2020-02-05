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
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;

import java.util.Map;

@CacheConfig(cacheNames = {"resources"})
public class CachedResourceService implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedResourceService.class);

    @Cacheable(key = "#resource")
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        LOGGER.info("Cache miss for resource {}", resource);
        throw new RuntimeException(String.format("No resource matching %s found in cache", resource));
    }

    @Cacheable(key = "#id")
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        LOGGER.info("Cache miss for resourceId {}", resourceId);
        throw new RuntimeException(String.format("No resource matching %s found in cache", resourceId));
    }

    @Cacheable(key = "#type")
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        LOGGER.info("Cache miss for resourceType {}", resourceType);
        throw new RuntimeException(String.format("No resource matching %s found in cache", resourceType));
    }

    @Cacheable(key = "#serialisedFormat")
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceFormat) {
        LOGGER.info("Cache miss for resourceFormat {}", resourceFormat);
        throw new RuntimeException(String.format("No resource matching %s found in cache", resourceFormat));
    }

    @Caching(put = {
            @CachePut(key = "#resource"),
            @CachePut(key = "#resource.id"),
            @CachePut(key = "#resource.type"),
            @CachePut(key = "#resource.serialisedFormat")
    })
    public Resource addResource(final LeafResource resource) {
        LOGGER.info("Cache add for resourceId: {}", resource.getId());
        LOGGER.debug("Added resource {} to cache. (Keys: resource, id, type, serialisedFormat)", resource);
        return resource;
    }
}
