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

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/")
public class AuditController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditController.class);

    private final AuditService service;

    public AuditController(final AuditService service) {
        this.service = service;
    }

    @PostMapping(value = "/audit", consumes = "application/json", produces = "application/json")
    public Boolean AuditRequest(@RequestBody final AuditRequest request) {
        LOGGER.debug("Invoking GetUserRequest: {}", request);
        return this.audit(request).join();
    }

    public CompletableFuture<Boolean> audit(final AuditRequest request) {
        return service.audit(request);
    }

}
