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
package uk.gov.gchq.palisade.service.palisade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

/**
 * <p> A simple implementation of a Palisade Service that just connects up the Audit, Cache, User, Policy and Resource
 * services. </p> <p> It currently doesn't validate that the user is actually requesting the correct resources. It
 * should check the resources requested in getDataRequestConfig are the same or a subset of the resources passed in in
 * registerDataRequest. </p>
 */
public class SimplePalisadeService implements PalisadeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeService.class);

    private final AuditService auditService;
    private final PolicyService policyService;
    private final UserService userService;
    private final ResourceService resourceService;
    private final PersistenceLayer persistenceLayer;
    private final ResultAggregationService aggregationService;

    private final Executor executor;

    public SimplePalisadeService(final AuditService auditService, final UserService userService, final PolicyService policyService, final ResourceService resourceService,
                                 final PersistenceLayer persistenceLayer, final Executor executor, final ResultAggregationService resultAggregationService) {
        requireNonNull(auditService, "auditService");
        requireNonNull(userService, "userService");
        requireNonNull(policyService, "policyService");
        requireNonNull(resourceService, "resourceService");
        requireNonNull(persistenceLayer, "persistenceLayer");
        requireNonNull(executor, "executor");
        requireNonNull(resultAggregationService, "resultAggregationService");
        this.auditService = auditService;
        this.userService = userService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.persistenceLayer = persistenceLayer;
        this.executor = executor;
        this.aggregationService = resultAggregationService;
    }

    @Override
    public CompletableFuture<DataRequestResponse> registerDataRequest(final RegisterDataRequest request) {
        requireNonNull(request, "request");
        final RequestId originalRequestId = request.getId();
        LOGGER.debug("Registering data request: {}, {}", request, originalRequestId);

        final GetUserRequest userRequest = new GetUserRequest().userId(request.getUserId());
        userRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting user from userService: {}", userRequest);
        final CompletableFuture<User> user = userService.getUser(userRequest);

        final GetResourcesByIdRequest resourceRequest = new GetResourcesByIdRequest().resourceId(request.getResourceId());
        resourceRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting resources from resourceService: {}", resourceRequest);
        final CompletableFuture<Map<LeafResource, ConnectionDetail>> resources = resourceService.getResourcesById(resourceRequest);

        final RequestId requestId = new RequestId().id(request.getUserId().getId() + "-" + UUID.randomUUID().toString());
        GetPolicyRequest policyRequest = new GetPolicyRequest();
        try {
            policyRequest = policyRequest.user(user.get()).context(request.getContext()).resources(resources.get().keySet());
            policyRequest.setOriginalRequestId(originalRequestId);
        } catch (Exception ex) {
            LOGGER.error("Error occurred: {}", ex.getMessage());
        }
        LOGGER.debug("Getting policy from policyService: {}", request);
        CompletableFuture<MultiPolicy> multiPolicy = policyService.getPolicy(policyRequest);

        LOGGER.debug("Aggregating results for \nrequest: {}, \nuser: {}, \nresources: {}, \npolicy:{}, \nrequestID: {}, \noriginal requestID: {}", request, user.join(), resources.join(), multiPolicy.join(), requestId, originalRequestId);

        return aggregationService.aggregateDataRequestResults(
                request, user.join(), resources.join(), multiPolicy.join(), requestId, originalRequestId).toCompletableFuture();
    }

    @Override
    public CompletableFuture<DataRequestConfig> getDataRequestConfig(
            final GetDataRequestConfig request) {
        requireNonNull(request);
        requireNonNull(request.getToken());
        // extract resources from request and check they are a subset of the original RegisterDataRequest resources
        LOGGER.debug("Getting cached data: {}", request);

        return this.persistenceLayer.getAsync(request.getOriginalRequestId().getId());
    }

}
