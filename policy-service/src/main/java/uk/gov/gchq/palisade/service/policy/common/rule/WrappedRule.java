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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.Generated;
import uk.gov.gchq.palisade.service.policy.common.User;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * A WrappedRule is helper implementation of {@link Rule}. It is useful
 * when you need to set simple rules that don't require the {@link User} or {@link Context}.
 *
 * @param <T> The type of the record. In normal cases the raw data will be deserialised
 *            by the record reader before being passed to the {@link Rule#apply(Serializable, User, Context)}.
 */
@JsonPropertyOrder(value = {"class", "rule", "function", "predicate"}, alphabetic = true)
public class WrappedRule<T extends Serializable> implements Rule<T> {

    public static final String WRAPPED_RULE_WAS_INITIALISED_WITH_NULL = "WrappedRule was initialised with null.";
    public static final String RULE_STRING = "rule";
    public static final String FUNCTION_STRING = "function";
    public static final String PREDICATE_STRING = "predicate";
    private static final long serialVersionUID = 1L;
    private Rule<T> rule;
    private SerialisableUnaryOperator<T> function;
    private SerialisablePredicate<T> predicate;

    /**
     * Constructs a WrappedRule with a given simple function rule to apply.
     * Note - using this means your rule will not be given the User or Context.
     *
     * @param function the simple {@link UnaryOperator} rule to wrap.
     */
    public WrappedRule(final SerialisableUnaryOperator<T> function) {
        requireNonNull(function, WRAPPED_RULE_WAS_INITIALISED_WITH_NULL + FUNCTION_STRING);
        this.function = function;
    }

    /**
     * Constructs a WrappedRule with a given simple predicate rule to apply.
     * Note - using this means your rule will not be given the User or Context.
     *
     * @param predicate the simple {@link Predicate} rule to wrap.
     */
    public WrappedRule(final SerialisablePredicate<T> predicate) {
        requireNonNull(predicate, WRAPPED_RULE_WAS_INITIALISED_WITH_NULL + PREDICATE_STRING);
        this.predicate = predicate;
    }

    @JsonCreator
    public WrappedRule(@JsonProperty("rule") final Rule<T> rule,
                       @JsonProperty("function") final SerialisableUnaryOperator<T> function,
                       @JsonProperty("predicate") final SerialisablePredicate<T> predicate) {
        checkNullCount(rule, function, predicate);
        this.rule = rule;
        this.function = function;
        this.predicate = predicate;
    }

    private void checkNullCount(final Rule<T> rule, final UnaryOperator<T> function, final Predicate<T> predicate) {
        // needs improving with Jackson
        int nullCount = 0;
        if (rule == null) {
            nullCount++;
        }
        if (function == null) {
            nullCount++;
        }
        if (predicate == null) {
            nullCount++;
        }
        if (nullCount != 2) {
            throw new IllegalArgumentException("Only one constructor parameter can be non-null");
        }
    }

    @Override
    public T apply(final T obj, final User user, final Context context) {
        final T rtn;
        if (nonNull(rule)) {
            rtn = rule.apply(obj, user, context);
        } else if (nonNull(function)) {
            rtn = function.apply(obj);
        } else if (nonNull(predicate)) {
            final boolean test = predicate.test(obj);
            if (test) {
                rtn = obj;
            } else {
                rtn = null;
            }
        } else {
            rtn = obj;
        }
        return rtn;
    }


    @Generated
    public Rule<T> getRule() {
        return rule;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
    @Generated
    public UnaryOperator<T> getFunction() {
        return function;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
    @Generated
    public Predicate<T> getPredicate() {
        return predicate;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WrappedRule)) {
            return false;
        }
        final WrappedRule<?> that = (WrappedRule<?>) o;
        return Objects.equals(rule, that.rule) &&
                Objects.equals(function, that.function) &&
                Objects.equals(predicate, that.predicate);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(rule, function, predicate);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", WrappedRule.class.getSimpleName() + "[", "]")
                .add("rule=" + rule)
                .add("function=" + function)
                .add("predicate=" + predicate)
                .toString();
    }
}
