/*
 * Copyright 2018 Crown Copyright
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

package uk.gov.gchq.palisade.service.policy.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * This class contains the mapping of {@link LeafResource}'s to the applicable {@link policy}
 */
public class MultiPolicy {
    private Map<LeafResource, policy> policies = new HashMap<>();

    public MultiPolicy() {
        // no-args constructor required
    }

    /**
     * @param policies a mapping of {@link LeafResource}'s to the applicable {@link policy}
     * @return the {@link MultiPolicy}
     */
    public MultiPolicy policies(final Map<LeafResource, policy> policies) {
        requireNonNull(policies, "The policies cannot be set to null.");
        this.policies.clear();
        this.policies.putAll(policies);
        return this;
    }

    public Map<LeafResource, policy> getPolicies() {
        //never null
        return policies;
    }

    public void setPolicies(final Map<LeafResource, policy> policies) {
        policies(policies);
    }

    /**
     * Retrieves the {@link policy} associated with the given {@link LeafResource}.
     * If the resource does not exist then an empty {@link policy} will be returned.
     *
     * @param resource the resource that you want the {@link policy} for.
     * @return The {@link policy} for the given {@link LeafResource}.
     */
    public policy getPolicy(final LeafResource resource) {
        requireNonNull(resource, "Cannot search for a policy based on a null resource.");
        final policy policy = getPolicies().get(resource);
        requireNonNull(policy, "There are no policies for this resource.");
        return policy;
    }

    /**
     * Sets the given {@link policy} to the given {@link LeafResource} provided
     * there isn't already a {@link policy} assigned to that {@link LeafResource}.
     *
     * @param resource the resource that you want the {@link policy} for
     * @param policy   The {@link policy} for the given {@link LeafResource}
     */
    public void setPolicy(final LeafResource resource, final policy policy) {
        requireNonNull(resource, "Cannot set a policy to a null resource.");
        requireNonNull(policy, "Cannot set a null policy to a resource.");
        Map<LeafResource, policy> policyMap = getPolicies();
        if (policyMap.containsKey(resource)) {
            throw new IllegalArgumentException("Policy already exists for resource: " + resource);
        }

        policyMap.put(resource, policy);
    }

    /**
     * This extracts the list of record level rules from the {@link policy} attached to each {@link LeafResource}.
     *
     * @return a mapping of the {@link LeafResource}'s to the record level {@link Rules} from the policies.
     */
    @JsonIgnore
    public Map<LeafResource, Rules> getRuleMap() {
        Map<LeafResource, policy> policyMap = getPolicies();
        final Map<LeafResource, Rules> rules = new HashMap<>(policyMap.size());
        policyMap.forEach((r, p) -> rules.put(r, p.getRecordRules()));
        return rules;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiPolicy that = (MultiPolicy) o;

        return new EqualsBuilder()
                .append(policies, that.policies)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(47, 53)
                .append(policies)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("policies", policies)
                .toString();
    }
}
