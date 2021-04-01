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

package uk.gov.gchq.palisade.service.policy.common.rule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.gov.gchq.palisade.service.policy.common.Generated;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to encapsulate the list of {@link Rule}s that apply to a resource and is provided with a user
 * friendly message to explain what the set of rules are.
 *
 * @param <T> The type of data records that the rules will be applied to.
 */
@JsonPropertyOrder(value = {"message", "rules"}, alphabetic = true)
public class Rules<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String ID_CANNOT_BE_NULL = "The id field can not be null.";
    private static final String RULE_CANNOT_BE_NULL = "The rule can not be null.";
    public static final String NO_RULES_SET = "no rules set";

    private String message;
    private LinkedHashMap<String, Rule<T>> rulesMap;

    /**
     * Constructs an empty instance of Rules.
     */
    public Rules() {
        rulesMap = new LinkedHashMap<>();
        message = NO_RULES_SET;
    }

    /**
     * Overrides the rules with these new rules
     *
     * @param rules the rules to set
     * @return this Rules instance
     */
    @Generated
    public Rules<T> rules(final Map<String, Rule<T>> rules) {
        this.setRules(rules);
        return this;
    }


    @Generated
    public Rules<T> addRules(final Map<String, Rule<T>> rules) {
        requireNonNull(rules, "Cannot add null to the existing rules.");
        this.rulesMap.putAll(rules);
        return this;
    }

    /**
     * Sets a message.
     *
     * @param message user friendly message to explain what the set of rules are.
     * @return this Rules instance
     */
    @Generated
    public Rules<T> message(final String message) {
        this.setMessage(message);
        return this;
    }

    /**
     * Adds a rule.
     *
     * @param id   the unique rule id
     * @param rule the rule
     * @return this Rules instance
     */
    @Generated
    public Rules<T> addRule(final String id, final Rule<T> rule) {
        requireNonNull(id, ID_CANNOT_BE_NULL);
        requireNonNull(rule, RULE_CANNOT_BE_NULL);
        rulesMap.put(id, rule);
        return this;
    }

    /**
     * Adds a predicate rule.
     *
     * @param id   the unique rule id
     * @param rule the predicate rule
     * @return this Rules instance
     */
    @Generated
    public Rules<T> addPredicateRule(final String id, final PredicateRule<T> rule) {
        this.addRule(id, rule);
        return this;
    }

    /**
     * Adds a simple predicate rule that just takes the record and returns true or false. Note - using this means your
     * rule will not be given the User or Context.
     *
     * @param id   the unique rule id
     * @param rule the simple predicate rule
     * @return this Rules instance
     */
    @Generated
    public Rules<T> addSimplePredicateRule(final String id, final SerialisablePredicate<T> rule) {
        this.addRule(id, new WrappedRule<>(rule));
        return this;
    }

    /**
     * Adds a simple function rule that just takes the record and returns a modified record or null if the record should
     * be fully redacted. Note - using this means your rule will not be given the User or Context.
     *
     * @param id   the unique rule id
     * @param rule the simple function rule
     * @return this Rules instance
     */
    @Generated
    public Rules<T> addSimpleFunctionRule(final String id, final SerialisableUnaryOperator<T> rule) {
        this.addRule(id, new WrappedRule<>(rule));
        return this;
    }

    @Generated
    public String getMessage() {
        return message;
    }

    @Generated
    public void setMessage(final String message) {
        requireNonNull(message);
        this.message = message;
    }

    @Generated
    public Map<String, Rule<T>> getRules() {
        return rulesMap;
    }

    @Generated
    public void setRules(final Map<String, Rule<T>> rulesMap) {
        requireNonNull(rulesMap);
        this.rulesMap = new LinkedHashMap<>(rulesMap);
    }

    /**
     * Tests if this rule set if empty.
     *
     * @return {@code true} if this rule set contains at least one rule
     */
    public boolean containsRules() {
        return !rulesMap.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rules)) {
            return false;
        }
        Rules<?> other = (Rules<?>) o;
        LinkedList<Class<? extends Rule>> thisRuleClasses = this.rulesMap.values().stream()
                .map(Rule::getClass)
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<Class<? extends Rule>> otherRuleClasses = other.rulesMap.values().stream()
                .map(Rule::getClass)
                .collect(Collectors.toCollection(LinkedList::new));
        return thisRuleClasses.equals(otherRuleClasses);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(message, rulesMap);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", Rules.class.getSimpleName() + "[", "]")
                .add("message='" + message + "'")
                .add("rulesHashMap=" + rulesMap)
                .add(super.toString())
                .toString();
    }
}
