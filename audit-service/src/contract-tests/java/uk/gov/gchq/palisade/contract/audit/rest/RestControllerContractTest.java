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

package uk.gov.gchq.palisade.contract.audit.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;
import uk.gov.gchq.palisade.service.audit.service.AuditService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = AuditApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
 class RestControllerContractTest extends AuditTestCommon {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private Map<String, AuditService> serviceMap;

    List<AuditRequest> requests = List.of(
            readRequestCompleteAuditRequest(),
            readRequestExceptionAuditRequest(),
            registerRequestCompleteAuditRequest(),
            registerRequestExceptionAuditRequest()
    );

    @Test
     void testContextLoads() {
        assertThat(serviceMap).isNotNull();
        assertThat(serviceMap).isNotEmpty();
    }

    @Test
     void testIsUp() {
        final ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
     void testComponent() {
        requests.forEach(request -> {
            Boolean response = restTemplate.postForObject("/audit", request, Boolean.class);
            assertThat(response).isTrue();
        });
    }
}
