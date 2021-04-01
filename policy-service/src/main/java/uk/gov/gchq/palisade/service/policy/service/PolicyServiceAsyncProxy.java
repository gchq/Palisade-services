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

package uk.gov.gchq.palisade.service.policy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceResponse;
import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyResourceRules;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An asynchronous service for processing a cacheable method.
 */
public class PolicyServiceAsyncProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceAsyncProxy.class);
    private final PolicyServiceHierarchyProxy service;
    private final Executor executor;

    /**
     * Constructor for instantiating the PolicyServiceAsyncProxy
     *
     * @param service  the PolicyServiceCachingProxy
     * @param executor an executor for any {@link CompletableFuture}s
     */
    public PolicyServiceAsyncProxy(final PolicyServiceHierarchyProxy service, final Executor executor) {
        LOGGER.debug("Initialised the PolicyServiceAsyncProxy");
        this.service = service;
        this.executor = executor;
    }

    /**
     * Given a nullable request, unwrap and store the request if it is non-null, ignore it if it is null
     * Async call to the {@link PolicyServiceHierarchyProxy#getResourceRules(LeafResource)} takes a resource to
     * return the rules applied against the resource. This will be wrapped in the container
     * {@link AuditablePolicyResourceRules} to be used to filter the resources
     *
     * @param nullableRequest the resource the user wants access to
     * @return the rules for the LeafResource, if null then an exception will be thrown
     */
    public CompletableFuture<AuditablePolicyResourceRules> getResourceRules(final @Nullable PolicyRequest nullableRequest) {
        return Optional.ofNullable(nullableRequest)
                .map(request ->
                        CompletableFuture.supplyAsync(() -> service.getResourceRules(request.getResource()), executor)
                                .thenApply(rules -> AuditablePolicyResourceRules.Builder.create()
                                        .withPolicyRequest(request)
                                        .withRules(rules)
                                        .withNoErrors())
                                .exceptionally(e -> AuditablePolicyResourceRules.Builder.create()
                                        .withPolicyRequest(request)
                                        .withRules(null)
                                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(request, Collections.emptyMap()).withError(e))
                                )
                )
                .orElse(CompletableFuture.completedFuture(AuditablePolicyResourceRules.Builder.create()
                        .withPolicyRequest(null)
                        .withRules(null)
                        .withNoErrors()));
    }

    /**
     * Given a nullable request, unwrap and store the request if it is non-null, ignore it if it is null
     * Async call to the {@link PolicyServiceHierarchyProxy} getRecordRules that takes a resource and gets the record rules applied against it
     *
     * @param modifiedAuditable the resource the user wants access to
     * @return the record rules for the LeafResource, if null then an exception will be thrown
     */
    public CompletableFuture<AuditablePolicyRecordResponse> getRecordRules(final AuditablePolicyResourceResponse modifiedAuditable) {
        PolicyRequest nullableRequest = modifiedAuditable.getPolicyRequest();
        return Optional.ofNullable(nullableRequest)
                .map(request ->
                        CompletableFuture.supplyAsync(() -> service.getRecordRules(request.getResource()))
                                .thenApply(rules -> AuditablePolicyRecordResponse.Builder.create()
                                        .withPolicyResponse(PolicyResponse.Builder.create(request)
                                                .withRules(rules))
                                        .withNoErrors())
                                .exceptionally(e -> AuditablePolicyRecordResponse.Builder.create()
                                        .withPolicyResponse(PolicyResponse.Builder.create(request)
                                                .withRules(new Rules<>()))
                                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(request, Collections.emptyMap()).withError(e)))
                )
                .orElse(CompletableFuture.completedFuture(AuditablePolicyRecordResponse.Builder.create()
                        .withPolicyResponse(null)
                        .withNoErrors()));
    }

    /**
     * Applies the {@link Rules} to the {@link Resource}.
     *
     * @param auditablePolicyResourceRules container for holding the {@code PolicyRequest}, {@code Rules} and the {@code AuditErrorMessage}
     * @return an instance of {@code AuditablePolicyResourceResponse} with the rules applied to the {@code Resource}
     */
    public static AuditablePolicyResourceResponse applyRulesToResource(final AuditablePolicyResourceRules auditablePolicyResourceRules) {
        Rules<LeafResource> rules = auditablePolicyResourceRules.getRules();
        PolicyRequest policyRequest = auditablePolicyResourceRules.getPolicyRequest();
        if (!Objects.isNull(rules)) {
            //apply the rules to the resource - a coarse grain filtering
            Resource resource = PolicyServiceHierarchyProxy.applyRulesToResource(policyRequest.getUser(), policyRequest.getResource(), policyRequest.getContext(), rules);
            return AuditablePolicyResourceResponse.Builder.create(auditablePolicyResourceRules).withModifiedResource(resource);
        } else {
            // do nothing and return
            return AuditablePolicyResourceResponse.Builder.create(auditablePolicyResourceRules).withNoModifiedResource();
        }
    }

}
