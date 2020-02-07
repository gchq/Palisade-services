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

import java.util.NoSuchElementException;
import java.util.Optional;

@CacheConfig(cacheNames = "policies")
public class CachedPolicyService implements PolicyService {
    private static Logger LOGGER = LoggerFactory.getLogger(CachedPolicyService.class);

    @Override
    @Cacheable(key="''.concat(#user.userId.id).concat('-').concat(#context.hashCode()).concat('-').concat(#resource.id)")
    public Optional<Resource> canAccess(final User user, final Context context, final Resource resource) {
        throw new NoSuchElementException(String.format("%s-%s-%s not found in cache", user.getUserId().getId(), context.hashCode(), resource.getId()));
    }

    @CachePut(key="''.concat(#user.userId.id).concat('-').concat(#context.hashCode()).concat('-').concat(#resource.id)")
    public Optional<Resource> cacheCanAccess(final User user, final Context context, final Resource resource, Optional<Resource> cacheVal) {
        LOGGER.info("{}-{}-{} :: {} added to cache", user.getUserId().getId(), context.hashCode(), resource.getId(), cacheVal);
        return cacheVal;
    }

    @Override
    @Cacheable(key = "#resource.id")
    public Optional<Policy> getPolicy(final Resource resource) {
        LOGGER.info("{} not foind in cache", resource.getId());
        throw new NoSuchElementException(String.format("%s not found in cache", resource.getId()));
    }

    @Override
    @CachePut(key = "#resource.id")
    public Policy setResourcePolicy(final Resource resource, final Policy policy) {
        LOGGER.info("{} :: {} added to cache", resource.getId(), policy);
        return policy;
    }

    @Override
    public Policy setTypePolicy(final String type, final Policy policy) {
        // Not implemented
        throw new RuntimeException(String.format("%s::setTypePolicy not implemented", CachedPolicyService.class));
    }
}
