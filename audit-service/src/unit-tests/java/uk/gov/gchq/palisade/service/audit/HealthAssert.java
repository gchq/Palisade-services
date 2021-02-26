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
import org.assertj.core.api.MapAssert;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Health} specific assertions
 */
public class HealthAssert extends AbstractAssert<HealthAssert, Health> {

    /**
     * Creates a new <code>{@link HealthAssert}</code> to make assertions on actual
     * Health.
     *
     * @param actual the Health we want to make assertions on.
     */
    public HealthAssert(final Health actual) {
        super(actual, HealthAssert.class);
    }

    /**
     * Verifies that the actual Health's status is equal to the given one.
     *
     * @param status the given status to compare the actual Health's status to.
     * @return this assertion object.
     * @throws AssertionError if the actual Health's status is not equal to the
     *                        given one.
     */
    public HealthAssert hasStatus(final Status status) {
        isNotNull();
        assertThat(actual.getStatus()).isEqualTo(status);
        return this;
    }

    /**
     * Verifies that the actual Health's status is DOWN
     *
     * @return this assertion object
     * @throws AssertionError if the actual Health's status is not DOWN
     */
    public HealthAssert isDown() {
        isNotNull();
        assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
        return this;
    }

    /**
     * Verifies that the actual Health's status is UP.
     *
     * @return this assertion object.
     * @throws AssertionError if the actual Health's status is not UP
     */
    public HealthAssert isUp() {
        isNotNull();
        assertThat(actual.getStatus()).isEqualTo(Status.UP);
        return this;
    }

    /**
     * Verifies that the actual Health has some details
     *
     * @return this assertion object
     * @throws AssertionError if the actual Health does not have any details
     */
    public HealthAssert hasDetails() {
        isNotNull();
        details().isNotEmpty();
        return this;
    }

    /**
     * Verifies that the actual Health has no details
     *
     * @return this assertion object
     * @throws AssertionError if the actual Health has some details
     */
    public HealthAssert hasNoDetails() {
        isNotNull();
        assertThat(actual.getDetails()).isEmpty();
        return this;
    }

    /**
     * Returns the details from the actual Health
     *
     * @return the details from the actual Health
     */
    public MapAssert<String, Object> details() {
        return assertThat(actual.getDetails());
    }


}