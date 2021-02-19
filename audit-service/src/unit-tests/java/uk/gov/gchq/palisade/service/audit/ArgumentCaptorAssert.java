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
package uk.gov.gchq.palisade.service.audit;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion methods for {@code ResponseEntity}s.
 */
public class ArgumentCaptorAssert<T> extends AbstractAssert<ArgumentCaptorAssert<T>, ArgumentCaptor<T>> {

    /**
     * Create a new {@code ArgumentCaptorAssert} instance
     *
     * @param actual The actual instance being asserted
     */
    public ArgumentCaptorAssert(final ArgumentCaptor<T> actual) {
        super(actual, ArgumentCaptorAssert.class);
    }

    /**
     * Verifies that the actual ArgumentCaptor contains no values
     *
     * @return this assertion object
     */
    public ArgumentCaptorAssert<T> isEmpty() {
        isNotNull();
        if (!actual.getAllValues().isEmpty()) {
            failWithMessage("Expected argment captor to be empty");
        }
        return this;
    }

    /**
     * Verifies that the actual {@code ArgumentCaptor} contains only the given
     * values and nothing else, in any order and ignoring duplicates (i.e. once a
     * value is found, its duplicates are also considered found).
     *
     * @param expected The given values
     * @return this assertion object
     */
    public ArgumentCaptorAssert<T> containsOnly(final Object... expected) {
        isNotNull();
        values().containsOnly(expected);
        return this;
    }

    /**
     * Returns a new ListAssertion instance over the values stored in the actual
     * {@code ArgumentCaptor}
     *
     * @return a new ListAssertion instance over the values stored in the actual
     *         {@code ArgumentCaptor}
     */
    public ListAssert<Object> values() {
        return assertThat(actual.getAllValues());
    }

}