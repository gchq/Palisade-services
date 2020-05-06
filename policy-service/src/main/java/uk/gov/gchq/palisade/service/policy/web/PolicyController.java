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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.UserConfiguration;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping(path = "/")
public class PolicyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService service;
    private PolicyConfiguration policyConfig;
    private UserConfiguration userConfig;

    public PolicyController(final @Qualifier("controller") PolicyService service,
                            final PolicyConfiguration policyConfig,
                            final UserConfiguration userConfig) {
        this.service = service;
        this.setPolicyConfig(policyConfig);
        this.setUserConfig(userConfig);
    }

    @Generated
    public PolicyService getService() {
        return service;
    }

    @Generated
    public PolicyConfiguration getPolicyConfig() {
        return policyConfig;
    }

    @Generated
    public void setPolicyConfig(final PolicyConfiguration policyConfig) {
        requireNonNull(policyConfig);
        this.policyConfig = policyConfig;
    }

    @Generated
    public UserConfiguration getUserConfig() {
        return userConfig;
    }

    @Generated
    public void setUserConfig(final UserConfiguration userConfig) {
        requireNonNull(userConfig);
        this.userConfig = userConfig;
    }

    @PostMapping(value = "/canAccess", consumes = "application/json", produces = "application/json")
    public CanAccessResponse canAccess(@RequestBody final CanAccessRequest request) {
        LOGGER.info("Invoking canAccess: {}", request);
        Collection<LeafResource> resources = canAccess(request.getUser(), request.getContext(), request.getResources());
        return new CanAccessResponse().canAccessResources(resources);
    }

    public Collection<LeafResource> canAccess(final User user, final Context context, final Collection<LeafResource> resources) {
        LOGGER.info("Filtering out resources for user {} with context {}", user, context);
        return resources.stream()
                .map(resource -> service.canAccess(user, context, resource))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @PostMapping(value = "/getPolicySync", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, Rules> getPolicySync(@RequestBody final GetPolicyRequest request) {
        LOGGER.info("Invoking getPolicySync: {}", request);
        Collection<LeafResource> resources = canAccess(request.getUser(), request.getContext(), request.getResources());
        /* Having filtered out any resources the user doesn't have access to in the line above, we now build the map
         * of resource to record level rule policies. If there are resource level rules for a record then there SHOULD
         * be record level rules. Either list may be empty, but they should at least be present
         */
        return resources.stream()
                .map(resource -> service.getPolicy(resource).map(policy -> new SimpleEntry<>(resource, policy.getRecordRules())))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @PutMapping(value = "/setResourcePolicyAsync", consumes = "application/json", produces = "application/json")
    public void setResourcePolicyAsync(@RequestBody final SetResourcePolicyRequest request) {
        LOGGER.info("Invoking setResourcePolicyAsync: {}", request);
        service.setResourcePolicy(request.getResource(), request.getPolicy());
    }

    @PutMapping(value = "/setTypePolicyAsync", consumes = "application/json", produces = "application/json")
    public void setTypePolicyAsync(@RequestBody final SetTypePolicyRequest request) {
        LOGGER.info("Invoking setTypePolicyAsync: {}", request);
        service.setTypePolicy(request.getType(), request.getPolicy());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        // Add example Policies to the policy-service cache
        LOGGER.info("Prepopulating using policy config: {}", policyConfig.getClass());
        LOGGER.info("Prepopulating using user config: {}", userConfig.getClass());
        policyConfig.getPolicies().stream()
                .map(prepopulation -> prepopulation.build(userConfig.getUsers()))
                .peek(entry -> LOGGER.debug(entry.toString()))
                .forEach(entry -> service.setResourcePolicy(entry.getKey(), entry.getValue()));
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PolicyController.class.getSimpleName() + "[", "]")
                .add("service=" + service)
                .add("policyConfig=" + policyConfig)
                .add("userConfig=" + userConfig)
                .add(super.toString())
                .toString();
    }
}
