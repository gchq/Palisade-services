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
package uk.gov.gchq.palisade.contract.palisade.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.palisade.PalisadeApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringBoot Starter Actuator is a service loaded-up by adding the spring-boot-starter-actuator as a dependency to the
 * project and configured in the application.yaml file.  It is a service which provides information on the application
 * and is being used to monitor the "health" of the palisade service.  If there is an indication that this service has
 * fallen over, this information can be used to restore the service.
 */
@SpringBootTest(
        classes = {PalisadeApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"management.health.kafka.enabled=false"}
)
@ActiveProfiles("akka-test")
class HealthActuatorContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * RESTful test to confirm the Spring Health Actuator service is up and running.
     */
    @Test
    void testHealthActuatorServiceIsRunning() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = responseEntity.getBody();
        assertThat(body).contains("\"status\":\"UP\"");
    }

}
