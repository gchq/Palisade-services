/*
 * Copyright 2018-2021 Crown Copyright
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

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

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
     * @param resourceId the resourceId the user wants resource rules against
     * @return the resource rules that apply to the resource
     */
    @Cacheable(value = "resourceRules", key = "#resourceId")
    public Optional<Rules<LeafResource>> getResourceRules(final String resourceId) {
        LOGGER.info("Cache miss for resourceId {}", resourceId);
        return service.getResourceRules(resourceId);
    }

    /**
     * Using the resourceId as the key, adds the resource, and any resource rules against that resource, to the cache
     *
     * @param resourceId the resourceId the user wants to apply resource rules to
     * @param rules      the resource rules that apply to this resource
     * @return the resource rules that apply to this LeafResource
     */
    @CachePut(value = "resourceRules", key = "#resourceId")
    public Optional<Rules<LeafResource>> setResourceRules(final String resourceId, final Rules<LeafResource> rules) {
        LOGGER.info("Cache add for resourceId {} and rules message {}", resourceId, rules.getMessage());
        LOGGER.debug("ResourceRules {} added to cache", rules);
        return service.setResourceRules(resourceId, rules);
    }

    /**
     * Using the resourceId as they key, retrieves the resource from the cache, if the resource doesnt exist a message is sent to the logs
     * and a {@link PolicyService} getRecordRules() is called
     *
     * @param resourceId the resourceId the user wants record rules against
     * @return the record rules that apply to the resource
     */
    @Cacheable(value = "recordRules", key = "#resourceId")
    public Optional<Rules<Serializable>> getRecordRules(final String resourceId) {
        LOGGER.info("Cache miss for resourceId {}", resourceId);
        return service.getRecordRules(resourceId);
    }

    /**
     * Using the resourceId as the key, adds the resource, and any record rules against that resource, to the cache
     *
     * @param resourceId the resourceId the user wants to apply record rules to
     * @param rules      the record rules that apply to this resource
     * @return the record rules that apply to this LeafResource
     */
    @CachePut(value = "recordRules", key = "#resourceId")
    public Optional<Rules<Serializable>> setRecordRules(final String resourceId, final Rules<Serializable> rules) {
        LOGGER.info("Cache add for resourceId {} and rules message {}", resourceId, rules.getMessage());
        LOGGER.debug("RecordRules {} added to cache", rules);
        return service.setRecordRules(resourceId, rules);
    }
}
