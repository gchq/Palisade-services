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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.PredicateRule;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to store the information that is required by the policy
 * service but not needed by the rest of the palisade services. That includes
 * separating the rules that need to be applied at the resource level or the record level.
 *
 * @param <T> The Java class that the rules expect the records of data to be in
 *            the format of, e.g. a Policy Int has Rules Resource for coarse filtering
 *            and Rules Int for fine filtering of files containing Ints
 */
public class Policy<T> {
    private Rules<T> recordRules;
    private Rules<Resource> resourceRules;
    private User owner;

    // no-args constructor required
    public Policy() {
        recordRules = new Rules<>();
        resourceRules = new Rules<>();
    }

    public Policy<T> recordRules(final Rules<T> recordRules) {
        requireNonNull(recordRules, "The record level rules cannot be set to null.");
        this.recordRules = recordRules;
        return this;
    }

    public Policy<T> resourceRules(final Rules<Resource> resourceRules) {
        requireNonNull(resourceRules, "The resource level rules cannot be set to null.");
        this.resourceRules = resourceRules;
        return this;
    }

    @JsonIgnore
    public String getMessage() {
        return "Resource level rules: " + getResourceRules().getMessage() + ", record level rules: " + getRecordRules().getMessage();
    }

    public Rules<T> getRecordRules() {
        // will never be null
        return recordRules;
    }

    public void setRecordRules(final Rules<T> recordRules) {
        recordRules(recordRules);
    }

    public Rules<Resource> getResourceRules() {
        // will never be null
        return resourceRules;
    }

    public void setResourceRules(final Rules<Resource> resourceRules) {
        resourceRules(resourceRules);
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private void addMessage(final String newMessage, final Rules rules) {
        requireNonNull(newMessage, "Cannot add a null message.");
        requireNonNull(rules, "Cannot add a message to a null set of rules.");
        String currentMessage = rules.getMessage();
        if (currentMessage.equals(Rules.NO_RULES_SET)) {
            rules.message(newMessage);
        } else {
            rules.message(currentMessage + ", " + newMessage);
        }
    }

    public Policy<T> recordLevelRule(final String message, final Rule<T> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<T> recordLevelRules = getRecordRules();
        recordLevelRules.rule(generateUUID(), rule);
        addMessage(message, recordLevelRules);
        return this;
    }

    public Policy<T> recordLevelPredicateRule(final String message, final PredicateRule<T> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<T> recordLevelRules = getRecordRules();
        recordLevelRules.rule(generateUUID(), rule);
        addMessage(message, recordLevelRules);
        return this;
    }

    public Policy<T> recordLevelSimplePredicateRule(final String message, final Predicate<T> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<T> recordLevelRules = getRecordRules();
        recordLevelRules.simplePredicateRule(generateUUID(), rule);
        addMessage(message, recordLevelRules);
        return this;
    }

    public Policy<T> recordLevelSimpleFunctionRule(final String message, final UnaryOperator<T> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<T> recordLevelRules = getRecordRules();
        recordLevelRules.simpleFunctionRule(generateUUID(), rule);
        addMessage(message, recordLevelRules);
        return this;
    }

    public Policy<T> resourceLevelRule(final String message, final Rule<Resource> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<Resource> resourceLevelRules = getResourceRules();
        resourceLevelRules.rule(generateUUID(), rule);
        addMessage(message, resourceLevelRules);
        return this;
    }

    public Policy<T> resourceLevelPredicateRule(final String message, final PredicateRule<Resource> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<Resource> resourceLevelRules = getResourceRules();
        resourceLevelRules.rule(generateUUID(), rule);
        addMessage(message, resourceLevelRules);
        return this;
    }

    public Policy<T> resourceLevelSimplePredicateRule(final String message, final Predicate<Resource> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<Resource> resourceLevelRules = getResourceRules();
        resourceLevelRules.simplePredicateRule(generateUUID(), rule);
        addMessage(message, resourceLevelRules);
        return this;
    }

    public Policy<T> resourceLevelSimpleFunctionRule(final String message, final UnaryOperator<Resource> rule) {
        requireNonNull(message, "The message cannot be null and should indicate what the rule is doing.");
        requireNonNull(rule, "Cannot set a null rule.");
        Rules<Resource> resourceLevelRules = getResourceRules();
        resourceLevelRules.simpleFunctionRule(generateUUID(), rule);
        addMessage(message, resourceLevelRules);
        return this;
    }

    public User getOwner() {
        requireNonNull(owner, "The owner has not been set.");
        return owner;
    }

    public void setOwner(final User owner) {
        owner(owner);
    }

    public Policy<T> owner(final User owner) {
        requireNonNull(owner, "The owner cannot be set to null.");
        this.owner = owner;
        return this;
    }

    @JsonGetter("owner")
    User getNullableOwner() {
        return owner;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Policy)) {
            return false;
        }
        final Policy<?> policy = (Policy<?>) o;
        return Objects.equals(recordRules, policy.recordRules) &&
                Objects.equals(resourceRules, policy.resourceRules) &&
                Objects.equals(owner, policy.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordRules, resourceRules, owner);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceRules", resourceRules)
                .append("recordRules", recordRules)
                .append("owner", owner)
                .toString();
    }
}
