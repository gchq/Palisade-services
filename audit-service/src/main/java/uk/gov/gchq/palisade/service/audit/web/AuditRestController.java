/*
 * Copyright 2019 Crown Copyright
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.audit.model.AuditRequest;
import uk.gov.gchq.palisade.service.audit.service.AuditService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RESTful interface to a number of different {@link AuditService}s
 */
@RestController
@RequestMapping(path = "/api")
public class AuditRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRestController.class);

    private final Map<String, AuditService> services;

    /**
     * Constructor taking in a collection of named {@link AuditService}s, auditing to each of them per request
     *
     * @param services a {@link Map} of services to use for this controller
     */
    public AuditRestController(final Map<String, AuditService> services) {
        this.services = services;
    }

    /**
     * Audit an incoming REST POST {@link AuditRequest} on the /audit endpoint.
     * Consumes and produces JSON requests and responses
     *
     * @param request the request to pass to each of the underlying services
     * @return true if all services completed their audit successfully, otherwise false
     */
    @PostMapping(value = "/audit", consumes = "application/json", produces = "application/json")
    public Boolean auditRequest(@RequestBody final AuditRequest request) {
        LOGGER.debug("Invoking GetUserRequest: {}", request);
        // Submit audit to all providing services
        final List<CompletableFuture<Boolean>> audits = this.audit(request);
        // Wait for all providers to complete
        // Succeed only if all providers succeeded
        boolean result = audits.stream().allMatch(CompletableFuture::join);
        LOGGER.debug("AuditRequest result is {}", result);
        return result;
    }

    /**
     * Asynchronously audit a request with all underlying services in parallel
     *
     * @param request the request to pass to each of the underlying services
     * @return a list of futures for whether each service completed and whether it was a successful completion
     */
    public List<CompletableFuture<Boolean>> audit(final AuditRequest request) {
        List<CompletableFuture<Boolean>> result = services.values().stream().map(
                auditService -> auditService.audit(request)
        ).collect(Collectors.toList());
        LOGGER.debug("audit result is {}", result);
        return result;
    }

}
