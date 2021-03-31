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
package uk.gov.gchq.palisade.service.attributemask.common.rule;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.User;

import java.io.Serializable;

/**
 * <p>
 * A {@code Rule} is the fundamental interface for applying policy criteria.
 * It allows a record to redacted or modified based on a {@link User} and their
 * query {@link Context}.
 * </p>
 * <p>
 * Please note, the context is optional and may be empty.
 * </p>
 * <p>
 * To work over the REST API implementations should be JSON serialisable.
 * The easiest way to achieve this is to have a default constructor and getters/setters for all fields.
 * </p>
 *
 * @param <T> The type of the record. In normal cases the raw data will be deserialised
 *            by the record reader before being passed to the apply(T, User, Context) method.
 */
@FunctionalInterface
@JsonPropertyOrder(value = {"class"}, alphabetic = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = As.EXISTING_PROPERTY,
        property = "class"
)
public interface Rule<T extends Serializable> extends Serializable {
    /**
     * Applies the rule logic to redact or modify the record based on the user and context.
     *
     * @param record  the record to be checked.
     * @param user    the user
     * @param context the query context
     * @return the modified record or null if the record should be fully redacted.
     */
    T apply(final T record, final User user, final Context context);

    /**
     * Used to indicate that this rule needs to be applied to the record.
     * If false, the application of this rule may be skipped.
     * This allows skipping resource deserialisation if all record rules are not applicable.
     *
     * @param user    the user
     * @param context the query context
     * @return true if the rule does need to be applied false if this rule can be bypassed
     */
    default boolean isApplicable(final User user, final Context context) {
        return true;
    }

    @JsonGetter("class")
    default String getClassName() {
        return getClass().getName();
    }

    @JsonSetter("class")
    default void setClassName(final String className) {
        // do nothing.
    }
}
