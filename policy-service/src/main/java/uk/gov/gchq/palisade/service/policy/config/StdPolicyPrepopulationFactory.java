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

package uk.gov.gchq.palisade.service.policy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.policy.common.Generated;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.policy.common.util.ResourceBuilder;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of a ResourcePrepopulationFactory that uses Spring to configure a resource from a yaml file
 * A factory for objects, using:
 * - a String reference of a {@link Resource} resource
 * - a map of {@link Rule} resource-level rules operating on a {@link Resource}
 * - a map of {@link Rule} record-level rules operating on the type of a {@link LeafResource}
 */
public class StdPolicyPrepopulationFactory implements PolicyPrepopulationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdPolicyPrepopulationFactory.class);

    private String resourceId;
    private Map<String, String> resourceRules;
    private Map<String, String> recordRules;

    /**
     * Empty constructor
     */
    public StdPolicyPrepopulationFactory() {
        resourceId = "";
        resourceRules = Collections.emptyMap();
        recordRules = Collections.emptyMap();
    }

    /**
     * Create a StdPolicyPrepopulationFactory, passing each member as an argument
     *
     * @param resourceId    the id of the {@link Resource} to attach a policy to
     * @param resourceRules the {@link Rule}s that apply to this {@link Resource} - used for canAccess requests
     * @param recordRules   the {@link Rule}s that apply to the record represented by this {@link Resource} - used by the data-service
     */
    public StdPolicyPrepopulationFactory(final String resourceId, final Map<String, String> resourceRules, final Map<String, String> recordRules) {
        this.resourceId = resourceId;
        this.resourceRules = resourceRules;
        this.recordRules = recordRules;
    }

    private static Rule createRule(final String rule) {
        try {
            LOGGER.debug("Adding rule {}", rule);
            return (Rule<?>) Class.forName(rule).getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            LOGGER.error(String.format("Error creating rule %s", rule), ex);
            return null;
        }
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public void setResourceId(final String resourceId) {
        this.resourceId = Optional.ofNullable(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("resource cannot be null"));
    }

    @Generated
    public Map<String, String> getResourceRules() {
        return resourceRules;
    }

    @Generated
    public void setResourceRules(final Map<String, String> resourceRules) {
        this.resourceRules = Optional.ofNullable(resourceRules)
                .orElseThrow(() -> new IllegalArgumentException("resourceRules cannot be null"));
    }

    @Generated
    public Map<String, String> getRecordRules() {
        return recordRules;
    }

    @Generated
    public void setRecordRules(final Map<String, String> recordRules) {
        this.recordRules = Optional.ofNullable(recordRules)
                .orElseThrow(() -> new IllegalArgumentException("recordRules cannot be null"));
    }

    @Override
    public Entry<String, Rules<LeafResource>> buildResourceRules() {
        Rules<LeafResource> rules = new Rules<>();
        resourceRules.forEach((message, rule) -> rules.addRule(message, createRule(rule)));

        // Interpret relative paths and prepend 'file:'
        String resourceUriId = ResourceBuilder.create(resourceId).getId();
        return new SimpleImmutableEntry<>(resourceUriId, rules);
    }

    @Override
    public Entry<String, Rules<Serializable>> buildRecordRules() {
        Rules<Serializable> rules = new Rules<>();
        recordRules.forEach((message, rule) -> rules.addRule(message, createRule(rule)));

        // Interpret relative paths and prepend 'file:'
        String resourceUriId = ResourceBuilder.create(resourceId).getId();
        return new SimpleImmutableEntry<>(resourceUriId, rules);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StdPolicyPrepopulationFactory that = (StdPolicyPrepopulationFactory) o;
        return Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(resourceRules, that.resourceRules) &&
                Objects.equals(recordRules, that.recordRules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resourceId, resourceRules, recordRules);
    }
}
