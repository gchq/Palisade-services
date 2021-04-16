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

package uk.gov.gchq.palisade.contract.audit.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An external requirement of the service is to keep-alive in k8s.
 * This is done by checking the service is still alive and healthy by REST GET /actuator/health.
 * This should return 200 OK if the service is healthy.
 */
@SpringBootTest(
        classes = AuditApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"management.health.kafka.enabled=false"}
)
@ActiveProfiles({"akka-test"})
class HealthActuatorContractTest {

    @Test
    void testContextLoads(@Autowired final Map<String, AuditService> serviceMap) {
        assertThat(serviceMap)
                .as("Check the Audit Service loads three different implementations of the Audit Service")
                .containsOnlyKeys("simple", "stroom", "logger");
    }

    @Test
    void testIsUp(@Autowired final TestRestTemplate restTemplate) {

        // When we GET the /actuator/health REST endpoint (used by k8s)
        var responseEntity = restTemplate.getForEntity("/actuator/health", String.class);

        // Then check the response
        assertThat(responseEntity.getStatusCode())
                .as("check status")
                .isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .as("check body")
                .contains("\"status\":\"UP\"");

    }
}
