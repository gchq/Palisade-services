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

package uk.gov.gchq.palisade.service.policy.common.util;

import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.rule.Rule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.common.user.User;

import java.io.Serializable;

import static java.util.Objects.isNull;

/**
 * Common utility methods.
 */
public final class RulesUtil {

    /**
     * Empty constructor
     */
    private RulesUtil() {
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
