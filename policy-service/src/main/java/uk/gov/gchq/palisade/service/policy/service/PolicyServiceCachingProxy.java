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

package uk.gov.gchq.palisade.service.policy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.Optional;

/**
 * This acts as a caching layer on top of an implementation of a policy service. This does not have any hierarchy logic
 * as that is handled before it reaches caching. In this way, a hierarchical traversal for policies is more
 * cache-friendly.
 * The three caches are used as follows:
 * - resourcePolicy is a map of Resources to Policies, another proxy may query for each parent resource etc.
 * - typePolicy is a map of Types to Policies
 * - accessPolicy is a map of CanAccessRequests (or equivalent of) to an Optional (available) Resource
 */
@CacheConfig(cacheNames = {"resourcePolicy, typePolicy, accessPolicy"})
public class PolicyServiceCachingProxy implements PolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceCachingProxy.class);
    private final PolicyService service;

    public PolicyServiceCachingProxy(final PolicyService service) {
        this.service = service;
    }

    @Override
    @Cacheable(value = "accessPolicy", key = "''.concat(#resource.getId()).concat('-').concat(#context.hashCode()).concat('-').concat(#user.hashCode())")
    public Optional<Resource> canAccess(final User user, final Context context, final Resource resource) {
        LOGGER.debug("Key triplet {}-{}-{} not found in cache", user.hashCode(), context.hashCode(), resource.hashCode());
        LOGGER.info("Cache miss for canAccess user {}, resource {}, context {}", user.getUserId(), resource.getId(), context);
        return service.canAccess(user, context, resource);
    }

    @Override
    @Cacheable(value = "resourcePolicy", key = "#resource.id")
    public Optional<Policy> getPolicy(final Resource resource) {
        LOGGER.debug("ResourceId for resource {} not found in cache", resource);
        LOGGER.info("Cache miss for resourceId {}", resource.getId());
        return service.getPolicy(resource);
    }

    @Override
    @CachePut(value = "resourcePolicy", key = "#resource.id")
    @CacheEvict(value = "accessPolicy")
    public <T> Policy<T> setResourcePolicy(final Resource resource, final Policy<T> policy) {
        LOGGER.debug("ResourceId for {} with policy {} added to cache", resource, policy);
        LOGGER.info("Cache add for resourceId {} and policy message {}", resource.getId(), policy.getMessage());
        return service.setResourcePolicy(resource, policy);
    }

    @Override
    @CachePut(value = "typePolicy", key = "#type")
    @CacheEvict(value = "accessPolicy")
    public <T> Policy<T> setTypePolicy(final String type, final Policy<T> policy) {
        LOGGER.debug("Type {} with policy {} added to cache", type, policy);
        LOGGER.info("Cache add for type {} with policy message {}", type, policy.getMessage());
        return service.setTypePolicy(type, policy);
    }
}
