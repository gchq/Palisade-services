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

package uk.gov.gchq.palisade.contract.audit.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.audit.web.SerDesHealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;

class SerDesHealthIndicatorTest {

    private final SerDesHealthIndicator healthIndicator = new SerDesHealthIndicator();
    @Autowired
    private SerDesConfig serDesConfig;

    @Test
    void testHealthUp() {
        // Given there are no errors

        // When getting the health
        Health actual = healthIndicator.health();

        // Then check the health is UP
        assertThat(actual.getStatus()).as("Check the health of the service").isEqualTo(Status.UP);
    }

    @Test
    void testHealthDown() {
        // Given the service has encountered at least 1 error
        SerDesConfig.setSerDesExceptions(new Exception("This is a test"));

        // When getting the health
        Health actual = healthIndicator.health();

        // Then check the health is DOWN
        assertThat(actual.getStatus()).as("Check the health of the service").isEqualTo(Status.DOWN);
    }
}
