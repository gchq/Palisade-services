/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.contract.attributemask.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
class HealthContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void serviceIsHealthy() {
        // Given that the service is running (and presumably healthy)

        // When we GET the /actuator/health REST endpoint (used by k8s)
        final ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);

        // Then the service reports itself to be healthy
        assertThat(health.getStatusCodeValue()).isEqualTo(200);
    }

}
