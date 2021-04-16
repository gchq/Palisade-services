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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.config.ApplicationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class RulesTest {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();


    @Test
    void testThatUpdatingDifferentRules() {
        // Given
        final Rules<String> ruleOne = new Rules<>();
        ruleOne.addRule("ruleOne", new TestRule());

        final Rules<String> ruleTwo = new Rules<>();
        ruleTwo.addRule("ruleTwo", new TestRule());

        // Then
        assertThat(ruleOne.getRules())
                .as("The 2 different rules should not match")
                .isNotEqualTo(ruleTwo.getRules());
    }

    @Test
    void testUpdatingTheSameRule() {
        final Rules<String> ruleOne = new Rules<>();
        ruleOne.addRule("one", new TestRule());

        final Rules<String> ruleTwo = new Rules<>();
        ruleTwo.addRule("one", new TestRule());

        assertThat(ruleOne)
                .as("The two rules should match")
                .isEqualTo(ruleTwo);
    }

    @Test
    void testSerialisingRuleEqualsString() throws JsonProcessingException {
        var rules = new Rules<String>()
                .message("Age off and visibility filtering")
                .addRule("ageOffRule", new TestRule()
                );

        final String testRule = "{\"message\":\"Age off and visibility filtering\",\"rules\":{\"ageOffRule\":{\"@type\":\"TestRule\"}}}";

        assertThat(testRule)
                .as("The string testRule should match the serialised json rule object")
                .isEqualTo(MAPPER.writeValueAsString(rules));
    }
}
