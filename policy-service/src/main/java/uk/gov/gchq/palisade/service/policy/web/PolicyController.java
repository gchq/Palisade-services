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
package uk.gov.gchq.palisade.service.policy.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/")
public class PolicyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyController.class);

    private final uk.gov.gchq.palisade.service.policy.service.PolicyService service;

    public PolicyController(final uk.gov.gchq.palisade.service.policy.service.PolicyService service) {
        this.service = service;
    }

    @PostMapping(value = "/canAccess", consumes = "application/json", produces = "application/json")
    public CanAccessResponse registerDataRequestSync(@RequestBody final CanAccessRequest request) {
        LOGGER.debug("Invoking registerDataRequest: {}", request);
        return this.canAccess(request).join();
    }

    public CompletableFuture<CanAccessResponse> canAccess(final CanAccessRequest request) {
        return service.canAccess(request);
    }

    @PostMapping(value = "/getPolicySync", consumes = "application/json", produces = "application/json")
    public MultiPolicy getPolicySync(@RequestBody final GetPolicyRequest request) {
        LOGGER.debug("Invoking getPolicy: {}", request);
        return getPolicy(request).join();
    }

    public CompletableFuture<MultiPolicy> getPolicy(final GetPolicyRequest request) {
        return service.getPolicy(request);
    }


    @PutMapping(value = "/setResourcePolicySync", consumes = "application/json", produces = "application/json")
    public void setResourcePolicySync(@RequestBody final SetResourcePolicyRequest request) {
        setResourcePolicy(request).join();
    }

    @PutMapping(value = "/setResourcePolicyAsync", consumes = "application/json", produces = "application/json")
    public void setResourcePolicyAsync(final SetResourcePolicyRequest request) {
        setResourcePolicy(request);
    }


    public CompletableFuture<Boolean> setResourcePolicy(final SetResourcePolicyRequest request) {
        LOGGER.debug("Invoking setResourcePolicy: {}", request);
        return service.setResourcePolicy(request);
    }


    @PutMapping(value = "/setTypePolicyAsync", consumes = "application/json", produces = "application/json")
    public void setTypePolicyAsync(final SetTypePolicyRequest request) {
        setTypePolicy(request);
    }

    public CompletableFuture<Boolean> setTypePolicy(final SetTypePolicyRequest request) {
        LOGGER.debug("Invoking setTypePolicy: {}", request);
        return service.setTypePolicy(request);
    }
}
