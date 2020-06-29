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

package uk.gov.gchq.palisade.service.policy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a {@link ResourcePrepopulationFactory} that uses Spring to configure a resource from a yaml file
 * A factory for {@link uk.gov.gchq.palisade.service.request.Policy} objects, using:
 * - a {@link uk.gov.gchq.palisade.resource.Resource} resource
 * - a {@link uk.gov.gchq.palisade.User} owner
 * - a list of {@link uk.gov.gchq.palisade.rule.Rule} resource-level rules operating on a {@link uk.gov.gchq.palisade.resource.Resource}
 * - a list of {@link uk.gov.gchq.palisade.rule.Rule} record-level rules operating on the type of a {@link uk.gov.gchq.palisade.resource.LeafResource}
 */
public class StdPolicyPrepopulationFactory implements PolicyPrepopulationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdPolicyPrepopulationFactory.class);

    private String resource;
    private String owner;
    private Map<String, String> resourceRules;
    private Map<String, String> recordRules;

    /**
     * Empty constructor
     */
    public StdPolicyPrepopulationFactory() {
        resource = "";
        owner = "";
        resourceRules = Collections.emptyMap();
        recordRules = Collections.emptyMap();
    }

    /**
     * Create a StdPolicyPrepopulationFactory, passing each member as an argument
     *
     * @param resource the {@link Resource} to attach a {@link Policy} to
     * @param owner the {@link User} of this {@link Policy}
     * @param resourceRules the {@link Rule}s that apply to this {@link Resource} - used for canAccess requests
     * @param recordRules the {@link Rule}s that apply to the record represented by this {@link Resource} - used by the data-service
     */
    public StdPolicyPrepopulationFactory(final String resource, final String owner, final Map<String, String> resourceRules, final Map<String, String> recordRules) {
        this.resource = resource;
        this.owner = owner;
        this.resourceRules = resourceRules;
        this.recordRules = recordRules;
    }

    @Generated
    public String getResource() {
        return resource;
    }

    @Generated
    public void setResource(final String resource) {
        requireNonNull(resource);
        this.resource = resource;
    }

    @Generated
    public String getOwner() {
        return owner;
    }

    @Generated
    public void setOwner(final String owner) {
        requireNonNull(owner);
        this.owner = owner;
    }

    @Generated
    public Map<String, String> getResourceRules() {
        return resourceRules;
    }

    @Generated
    public void setResourceRules(final Map<String, String> resourceRules) {
        requireNonNull(resourceRules);
        this.resourceRules = resourceRules;
    }

    @Generated
    public Map<String, String> getRecordRules() {
        return recordRules;
    }

    @Generated
    public void setRecordRules(final Map<String, String> recordRules) {
        requireNonNull(recordRules);
        this.recordRules = recordRules;
    }

    @Override
    public Entry<Resource, Policy> build(final List<? extends UserPrepopulationFactory> users, final List<? extends ResourcePrepopulationFactory> resources) {
        Policy<?> policy = new Policy<>();

        resourceRules.forEach((message, rule) -> policy.resourceLevelRule(message, createRule(rule)));
        recordRules.forEach((message, rule) -> policy.recordLevelRule(message, createRule(rule)));

        Resource unconfiguredResource = ResourceBuilder.create(this.resource);
        Resource policyResource = resources.stream()
                .map(factory -> (Resource) factory.build(x -> new SimpleConnectionDetail().serviceName("")).getValue())
                .filter(builtResource -> builtResource.getId().equals(unconfiguredResource.getId()))
                .findFirst()
                .orElse(unconfiguredResource);

        User policyOwner = users.stream()
                .map(UserPrepopulationFactory::build)
                .filter(user -> user.getUserId().getId().equals(this.owner))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find user with id: " + this.owner));

        return new SimpleImmutableEntry<>(policyResource, policy.owner(policyOwner));
    }

    private static Rule createRule(final String rule) {
        try {
            LOGGER.debug("Adding rule {}", rule);
            return (Rule<?>) Class.forName(rule).getConstructor().newInstance();
        } catch (Exception ex) {
            LOGGER.error(String.format("Error creating rule %s", rule), ex);
            return null;
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdPolicyPrepopulationFactory)) {
            return false;
        }
        final StdPolicyPrepopulationFactory that = (StdPolicyPrepopulationFactory) o;
        return Objects.equals(resource, that.resource) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(resourceRules, that.resourceRules) &&
                Objects.equals(recordRules, that.recordRules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resource, owner, resourceRules, recordRules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdPolicyPrepopulationFactory.class.getSimpleName() + "[", "]")
                .add("resource='" + resource + "'")
                .add("owner='" + owner + "'")
                .add("resourceRules=" + resourceRules)
                .add("recordRules=" + recordRules)
                .add(super.toString())
                .toString();
    }
}
