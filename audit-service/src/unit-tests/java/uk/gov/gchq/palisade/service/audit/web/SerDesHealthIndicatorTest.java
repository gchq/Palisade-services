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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
class SerDesHealthIndicatorTest {

    private SerDesHealthIndicator healthIndicator;

    @BeforeEach
    void setup() {
        healthIndicator = new SerDesHealthIndicator();
    }

    @AfterEach
    void tearDown() {
        SerDesHealthIndicator.SER_DES_EXCEPTIONS.clear();
    }

    @Test
    void testHealthUp() {
        assertThat(healthIndicator.health().getStatus())
                .as("Check health status is UP")
                .isEqualTo(Status.UP);
    }

    @Test
    void testHealthDown() {
        // Given the service has encountered at least 1 serialisation exception
        SerDesHealthIndicator.addSerDesExceptions(new Exception("This is a test"));
        assertThat(healthIndicator.health().getStatus())
                .as("Check health status as DOWN")
                .isEqualTo(Status.DOWN);
        assertThat(healthIndicator.health().getDetails())
                .as("Check health status has some details")
                .isNotEmpty();
    }
}
