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

import uk.gov.gchq.palisade.service.audit.request.AuditRequest;
import uk.gov.gchq.palisade.service.audit.service.AuditService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/")
public class AuditController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditController.class);

    private final Map<String, AuditService> services;

    public AuditController(final Map<String, AuditService> services) {
        this.services = services;
    }

    @PostMapping(value = "/audit", consumes = "application/json", produces = "application/json")
    public Boolean auditRequest(@RequestBody final AuditRequest request) throws ExecutionException, InterruptedException {
        LOGGER.debug("Invoking GetUserRequest: {}", request);
        // Submit audit to all providing services
        final List<CompletableFuture<Boolean>> audits = this.audit(request);
        // Wait for all providers to complete
        final CompletableFuture<List<Boolean>> results = CompletableFuture.allOf(audits.toArray(new CompletableFuture[0]))
                .thenApply(res -> audits.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        // Succeed only if all providers succeeded
        boolean result = results.get().stream().allMatch(res -> res);
        LOGGER.debug("AuditRequest result is {}", result);
        return result;
    }

    public List<CompletableFuture<Boolean>> audit(final AuditRequest request) {
        List<CompletableFuture<Boolean>> result = services.values().stream().map(
                auditService -> auditService.audit(request)
        ).collect(Collectors.toList());
        LOGGER.debug("audit result is {}", result);
        return result;
    }

}
