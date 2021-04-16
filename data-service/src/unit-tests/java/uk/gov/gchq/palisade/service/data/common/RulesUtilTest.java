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

package uk.gov.gchq.palisade.service.data.common;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.data.common.rule.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.gchq.palisade.service.data.common.util.RulesUtil.applyRulesToItem;

class RulesUtilTest {

    @Test
    void testTheReturnResourceIfNoRulesAreApplied() {
        // When
        final String actual1 = applyRulesToItem("String", null, null, null);
        final String actual2 = applyRulesToItem("String", null, null, new Rules<>());

        // Then
        assertThat(actual1)
                .as("Only 'String' should be returned if there are no rules")
                .isEqualTo("String");

        assertThat(actual2)
                .as("Only 'String' should be returned if there are no rules")
                .isEqualTo("String");

    }

    @Test
    void testShouldUpdateRecord() {
        // Given
        final Rules<String> rules = new Rules<String>().addRule("r1", (record, user, context) -> "fromRule");
        // When
        final String actual1 = applyRulesToItem("String", null, null, rules);

        // Then
        assertThat(actual1)
                .as("'fromRule' should be returned as the record has been updated")
                .isEqualTo("fromRule");
    }

    @Test
    void testUpdateRecordFromAllRules() {
        // Given
        final Rules<String> rules = new Rules<String>()
                .addRule("r1", (record, user, context) -> "fromRule")
                .addRule("r2", (record, user, context) -> record.concat("2ndRule"));

        // When
        final String actual1 = applyRulesToItem("String", null, null, rules);

        // Then
        assertThat(actual1)
                .as("'fromRule2ndRule' should be returned as the record has been updated for all rules")
                .isEqualTo("fromRule" + "2ndRule");
    }
}
