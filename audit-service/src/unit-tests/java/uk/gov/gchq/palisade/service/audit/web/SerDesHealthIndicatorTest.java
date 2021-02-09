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

package uk.gov.gchq.palisade.service.audit.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SerDesHealthIndicatorTest {

    private final SerDesHealthIndicator healthIndicator = new SerDesHealthIndicator();

    @AfterEach
    void tearDown() {
        SerDesHealthIndicator.SER_DES_EXCEPTIONS.clear();
    }

    @Test
    void testHealthUp() {
        // Given there are no errors

        // When getting the health
        Health actual = healthIndicator.health();

        // Then check the health is UP
        assertThat(actual.getStatus()).as("Check the health status is UP").isEqualTo(Status.UP);
    }

    @Test
    void testHealthDown() {
        // Given the service has encountered at least 1 serialisation exception
        SerDesHealthIndicator.addSerDesExceptions(new Exception("This is a test"));

        // When getting the health
        Health actual = healthIndicator.health();

        // Then check the health is DOWN
        assertAll(
                () -> assertThat(actual.getStatus()).as("Check the health status is DOWN").isEqualTo(Status.DOWN),
                () -> assertThat(actual.getDetails()).as("Check the health details are not empty").isNotEmpty()
        );
    }
}