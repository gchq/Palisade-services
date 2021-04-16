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

package uk.gov.gchq.palisade.service.data.common.rule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.Generated;
import uk.gov.gchq.palisade.service.data.common.user.User;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * A {@link WrappedRule} is helper implementation of {@link Rule}. It is useful
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
    private SerializableUnaryOperator<T> function;
    private SerializablePredicate<T> predicate;

    /**
     * Constructs a {@link WrappedRule} with a null rule.
     */
    public WrappedRule() {
    }

    /**
     * Constructs a {@link WrappedRule} with a given rule to wrap.
     *
     * @param rule the {@link Rule} to wrap.
     */
    public WrappedRule(final Rule<T> rule) {
        requireNonNull(rule, WRAPPED_RULE_WAS_INITIALISED_WITH_NULL + RULE_STRING);
        this.rule = rule;
    }

    /**
     * Constructs a {@link WrappedRule} with a given simple function rule to apply.
     * Note - using this means your rule will not be given the User or Context.
     *
     * @param function the simple {@link UnaryOperator} rule to wrap.
     */
    public WrappedRule(final SerializableUnaryOperator<T> function) {
        requireNonNull(function, WRAPPED_RULE_WAS_INITIALISED_WITH_NULL + FUNCTION_STRING);
        this.function = function;
    }

    /**
     * Constructs a {@link WrappedRule} with a given simple predicate rule to apply.
     * Note - using this means your rule will not be given the User or Context.
     *
     * @param predicate the simple {@link Predicate} rule to wrap.
     */
    public WrappedRule(final SerializablePredicate<T> predicate) {
        requireNonNull(predicate, WRAPPED_RULE_WAS_INITIALISED_WITH_NULL + PREDICATE_STRING);
        this.predicate = predicate;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
    @Generated
    public UnaryOperator<T> getFunction() {
        return function;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
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
