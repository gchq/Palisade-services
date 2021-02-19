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
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.ObjectAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion methods for {@code ResponseEntity}s.
 *
 * @param <T> The type contained in the response entity
 */
public class ResponseEntityAssert<T> extends AbstractAssert<ResponseEntityAssert<T>, ResponseEntity<T>> {

    /**
     * Create a new {@code ResponseEntityAssert} instance
     *
     * @param actual The actual instance being asserted
     */
    public ResponseEntityAssert(final ResponseEntity<T> actual) {
        super(actual, ResponseEntityAssert.class);
    }

    /**
     * Verifies that the actual {@code ResponseEntity}'s status code matches the
     * expected one
     *
     * @param statusCode The expected status code
     * @return this assert
     */
    public ResponseEntityAssert<T> hasStatusCode(final HttpStatus statusCode) {
        isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(statusCode);
        return this;
    }

    /**
     * Verifies that the actual {@code ResponseEntity}'s status code value matches
     * the expected value
     *
     * @param statusCodeValue The expected status code value
     * @return this assert
     */
    public ResponseEntityAssert<T> hasStatusCode(final int statusCodeValue) {
        isNotNull();
        assertThat(actual.getStatusCodeValue()).isEqualTo(statusCodeValue);
        return this;
    }

    /**
     * verifies that the actual {@code ResponseEntity} has a body
     *
     * @return this assert
     */
    public ResponseEntityAssert<T> hasBody() {
        isNotNull();
        assertThat(actual.hasBody()).isTrue();
        return this;
    }

    /**
     * Verifies that the actual {@code ResponseEntity}'s body matches the expected
     * one
     *
     * @param body expected body
     * @return this assert
     */
    public ResponseEntityAssert<T> hasBody(final T body) {
        isNotNull();
        assertThat(actual.getBody()).isEqualTo(body);
        return this;
    }

    /**
     * Returns actual {@code ResponseEntity}'s body as a string
     *
     * @return this assert
     */
    public AbstractStringAssert<?> bodyString() {
        return assertThat(actual.getBody().toString());
    }

    /**
     * Returns actual {@code ResponseEntity}'s body
     *
     * @return this assert
     */
    public ObjectAssert<T> body() {
        return assertThat(actual.getBody());
    }

}