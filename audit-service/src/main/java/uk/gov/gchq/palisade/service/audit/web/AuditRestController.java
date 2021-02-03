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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.service.KafkaProducerService;

import java.util.Collections;
import java.util.Map;

/**
 * A REST interface mimicking the Kafka API to the service.
 * POSTs to the controller write the request and headers to the upstream topic.
 * These messages will then later be read by the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping(path = "/api")
public class AuditRestController {

    private final KafkaProducerService service;

    /**
     * Autowired constructor for the rest controller
     *
     * @param service the service used to process an {@link AuditMessage}
     */
    public AuditRestController(final KafkaProducerService service) {
        this.service = service;
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the success request
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/success", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> auditRequest(final @RequestHeader Map<String, String> headers,
                                             final @RequestBody AuditSuccessMessage request) {

        // Process the request and return results
        return service.processSuccessRequest(headers, Collections.singletonList(request));

    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the error request
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/error", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> auditRequest(final @RequestHeader Map<String, String> headers,
                                             final @RequestBody AuditErrorMessage request) {

        // Process the request and return results
        return service.processErrorRequest(headers, Collections.singletonList(request));

    }
}
