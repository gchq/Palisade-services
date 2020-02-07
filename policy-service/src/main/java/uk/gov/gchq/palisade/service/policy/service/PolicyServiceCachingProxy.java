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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.Optional;

@CacheConfig(cacheNames = "policies")
public class PolicyServiceCachingProxy implements PolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceCachingProxy.class);
    private final PolicyService service;

    public PolicyServiceCachingProxy(final PolicyService service) {
        this.service = service;
    }

    @Override
    @Cacheable(key="''.concat(#user).concat('-').concat(#context).concat('-').concat(#resource)")
    public Optional<Resource> canAccess(final User user, final Context context, final Resource resource) {
        LOGGER.debug("Key triplet {}-{}-{} not found in cache", user, context, resource);
        return service.canAccess(user, context, resource);
    }

    // This is needed for canAccess calls to be cacheable but is never used
    @CachePut(key="''.concat(#user).concat('-').concat(#context).concat('-').concat(#resource)")
    public Optional<Resource> setAccess(final User user, final Context context, final Resource resource, Optional<Resource> cacheVal) {
        LOGGER.debug("Key triplet {}-{}-{} with access {} added to cache", user, context, resource, cacheVal);
        return cacheVal;
    }

    @Override
    @Cacheable(key = "#resource")
    public Optional<Policy> getPolicy(final Resource resource) {
        LOGGER.debug("Resource {} not found in cache", resource);
        return service.getPolicy(resource);
    }

    @Override
    @CachePut(key = "#resource")
    public <T> Policy<T> setResourcePolicy(final Resource resource, final Policy<T> policy) {
        LOGGER.debug("Resource {} with policy {} added to cache", resource, policy);
        return service.setResourcePolicy(resource, policy);
    }

    @Override
    @CachePut(key = "#type")
    public <T> Policy<T> setTypePolicy(final String type, final Policy<T> policy) {
        LOGGER.debug("Type {} with policy {} added to cache", type, policy);
        return service.setTypePolicy(type, policy);
    }
}
