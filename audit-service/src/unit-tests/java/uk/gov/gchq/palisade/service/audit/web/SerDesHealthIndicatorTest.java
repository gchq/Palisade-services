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

import static uk.gov.gchq.palisade.service.audit.Assertions.assertThat;

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
        assertThat(healthIndicator.health())
            .as("Check health status is UP")
            .isUp();
    }

    @Test
    void testHealthDown() {
        // service has encountered at least 1 serialisation exception
        SerDesHealthIndicator.addSerDesExceptions(new Exception("This is a test"));
        assertThat(healthIndicator.health())
            .as("Check health status as DOWN and has some details")
            .isDown()
            .hasDetails();
    }
}
