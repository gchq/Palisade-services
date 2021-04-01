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
package uk.gov.gchq.palisade.service.policy.common;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.policy.common.rule.SerialisableUnaryOperator;
import uk.gov.gchq.palisade.service.policy.common.rule.WrappedRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("java:S5778")
class WrappedRuleTest {

    @Test
    void testArgumentWithNoErrors() {
        // Given
        WrappedRule<String> rule1 = new WrappedRule<>(new TestRule(), null, null);
        WrappedRule<String> rule2 = new WrappedRule<>(null, o -> o, null);
        WrappedRule<String> rule3 = new WrappedRule<>(null, null, o -> true);

        // Then
        assertThat(rule1.getRule())
                .as("rule1 should not be null")
                .isInstanceOf(TestRule.class)
                .isNotNull();

        assertThat(rule2.getFunction())
                .as("rule2 function should not be null")
                .isNotNull();

        assertThat(rule3.getPredicate())
                .as("rule3 predicate should not be null")
                .isNotNull();
    }

    @Test
    void test0Arguments() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new WrappedRule<>(null, null, null));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("Only one constructor parameter can be non-null");
    }

    @Test
    void testNullPredicate() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new WrappedRule<>(new TestRule(), o -> o, null));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("Only one constructor parameter can be non-null");
    }

    @Test
    void testNullFunction() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new WrappedRule<>(new TestRule(), null, o -> true));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("Only one constructor parameter can be non-null");
    }

    @Test
    void testNullRule() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new WrappedRule<>(null, (SerialisableUnaryOperator<String>) String::toString, o -> true));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("Only one constructor parameter can be non-null");
    }

    @Test
    void test3Arguments() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new WrappedRule<>(new TestRule(), o -> o, o -> true));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("Only one constructor parameter can be non-null");
    }

}
