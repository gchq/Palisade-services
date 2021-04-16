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

package uk.gov.gchq.palisade.service.data.common.util;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.rule.Rule;
import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.common.user.User;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

/**
 * Common utility methods.
 */
@SuppressWarnings({"java:S112", "java:S2658", "java:S2142"})
public final class RulesUtil {

    private RulesUtil() {
    }

    /**
     * Applies a collection of rules to a record stream.
     *
     * @param records          record stream
     * @param user             user the records are being processed for
     * @param context          the additional context
     * @param rules            rules collection
     * @param <T>              record type
     * @param recordsProcessed a counter for the number of records being processed
     * @param recordsReturned  a counter for the number of records being returned
     * @return filtered stream
     */
    public static <T extends Serializable> Stream<T> applyRulesToStream(final Stream<T> records, final User user, final Context context, final Rules<T> rules, final AtomicLong recordsProcessed, final AtomicLong recordsReturned) {
        Objects.requireNonNull(records);
        if (isNull(rules) || isNull(rules.getRules()) || rules.getRules().isEmpty()) {
            return records;
        }

        return records
                .peek(processed -> recordsProcessed.incrementAndGet())
                .map(record -> applyRulesToItem(record, user, context, rules))
                .filter(Objects::nonNull)
                .peek(returned -> recordsReturned.incrementAndGet());
    }

    /**
     * Applies a collection of rules to an item (record/resource).
     *
     * @param item    resource or record to filter
     * @param user    user the record is being processed for
     * @param context the additional context
     * @param rules   rules collection
     * @param <T>     record type
     * @return item with rules applied
     */
    public static <T extends Serializable> T applyRulesToItem(final T item, final User user, final Context context, final Rules<T> rules) {
        if (isNull(rules) || isNull(rules.getRules()) || rules.getRules().isEmpty()) {
            return item;
        }
        T updateItem = item;
        for (final Rule<T> rule : rules.getRules().values()) {
            updateItem = rule.apply(updateItem, user, context);
            if (null == updateItem) {
                break;
            }
        }
        return updateItem;
    }
}
