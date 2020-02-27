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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;

import java.util.Map;

@CacheConfig(cacheNames = {"resources", "resourceId", "resourceType", "resourceFormat"})
public class ResourceServiceCachingProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceCachingProxy.class);

    private ResourceService service;

    public ResourceServiceCachingProxy(final ResourceService service) {
        this.service = service;
    }

    @Cacheable(value = "resources", key = "#resource")
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        LOGGER.info("Cache miss for resource {}", resource);
        return service.getResourcesByResource(resource);
    }

    @Cacheable(value = "resourceId", key = "#resourceId")
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        LOGGER.info("Cache miss for resourceId {}", resourceId);
        return service.getResourcesById(resourceId);
    }

   @Cacheable(value = "resourceType", key = "#resourceType")
    public  Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        LOGGER.info("Cache miss for resourceType {}", resourceType);
        return service.getResourcesByType(resourceType);
    }

    @Cacheable(value = "resourceFormat", key = "#serialisedFormat")
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String serialisedFormat) {
        LOGGER.info("Cache miss for resourceFormat {}", serialisedFormat);
        return service.getResourcesBySerialisedFormat(serialisedFormat);
    }

    @Caching(evict = {
            @CacheEvict(value = "resources", key = "#resource"),
            @CacheEvict(value = "resourceId", key = "#resource.id"),
            @CacheEvict(value = "resourceType", key = "#resource.type"),
            @CacheEvict(value = "resourceFormat", key = "#resource.serialisedFormat")
    })
    public Resource addResource(final Resource resource) {
        LOGGER.info("Cache evict for resource: {}", resource);
        return service.addResource(resource);
    }
}
