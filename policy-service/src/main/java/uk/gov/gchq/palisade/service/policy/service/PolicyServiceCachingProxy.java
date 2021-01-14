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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;

import java.io.Serializable;
import java.util.Optional;

/**
 * This acts as a caching layer on top of an implementation of a policy service. This does not have any hierarchy logic
 * as that is handled before it reaches caching. In this way, a hierarchical traversal for policies is more
 * cache-friendly.
 */
@CacheConfig(cacheNames = {"resourceRules, recordRules"})
public class PolicyServiceCachingProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceCachingProxy.class);
    private final PolicyService service;

    /**
     * Default constructor used to create the PolicyServiceCachingProxy
     *
     * @param service {@link PolicyService} this service calls
     */
    public PolicyServiceCachingProxy(final PolicyService service) {
        this.service = service;
    }

    /**
     * Using the resourceId as they key, retrieves the resource from the cache, if the resource doesnt exist a message is sent to the logs
     * and a {@link PolicyService} getResourceRules() is called
     *
     * @param resource the resource the user wants resource rules against
     * @return the resource rules that apply to the resource
     */
    @Cacheable(value = "resourceRules", key = "#resource.id")
    public Optional<Rules<LeafResource>> getResourceRules(final Resource resource) {
        LOGGER.info("Cache miss for resourceId {}", resource.getId());
        return service.getResourceRules(resource);
    }

    /**
     * Using the resourceId as the key, adds the resource, and any resource rules against that resource, to the cache
     *
     * @param resource the resource the user wants to apply resource rules to
     * @param rules    the resource rules that apply to this resource
     * @return the resource rules that apply to this LeafResource
     */
    @CachePut(value = "resourceRules", key = "#resource.id")
    public Optional<Rules<LeafResource>> setResourceRules(final Resource resource, final Rules<LeafResource> rules) {
        LOGGER.info("ResourceId for {} with policy {} added to cache", resource, rules);
        LOGGER.debug("Cache add for resourceId {} and policy message {}", resource.getId(), rules.getMessage());
        return service.setResourceRules(resource, rules);
    }

    /**
     * Using the resourceId as they key, retrieves the resource from the cache, if the resource doesnt exist a message is sent to the logs
     * and a {@link PolicyService} getRecordRules() is called
     *
     * @param resource the resource the user wants record rules against
     * @return the record rules that apply to the resource
     */
    @Cacheable(value = "recordRules", key = "#resource.id")
    public Optional<Rules<Serializable>> getRecordRules(final Resource resource) {
        LOGGER.info("ResourceId for resource {} not found in cache", resource);
        LOGGER.debug("Cache miss for resourceId {}", resource.getId());
        return service.getRecordRules(resource);
    }

    /**
     * Using the resourceId as the key, adds the resource, and any record rules against that resource, to the cache
     *
     * @param resource the resource the user wants to apply record rules to
     * @param rules    the record rules that apply to this resource
     * @return the record rules that apply to this LeafResource
     */
    @CachePut(value = "recordRules", key = "#resource.id")
    public Optional<Rules<Serializable>> setRecordRules(final Resource resource, final Rules<Serializable> rules) {
        LOGGER.info("ResourceId for {} with policy {} added to cache", resource, rules);
        LOGGER.debug("Cache add for resourceId {} and policy message {}", resource.getId(), rules.getMessage());
        return service.setRecordRules(resource, rules);
    }
}
