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
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.request.AddCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest.RegisterRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;
import uk.gov.gchq.palisade.service.request.Request;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
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
    //Cache keys
    public static final String RES_COUNT_KEY = "res_count_";

    /**
     * Duration for how long the count of resources requested should live in the cache.
     */
    public static final Duration COUNT_PERSIST_DURATION = Duration.ofMinutes(10);

    private final AuditService auditService;
    private final PolicyService policyService;
    private final UserService userService;
    private final ResourceService resourceService;
    private final CacheService cacheService;
    private ResultAggregationService aggregationService;

    private final Executor executor;

    public SimplePalisadeService(final AuditService auditService, final UserService userService, final PolicyService policyService, final ResourceService resourceService, final CacheService cacheService, final Executor executor) {
        this.auditService = auditService;
        this.userService = userService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.cacheService = cacheService;
        this.executor = executor;
        this.aggregationService = new ResultAggregationService(auditService, cacheService);
    }

    @Override
    public CompletableFuture<DataRequestResponse> registerDataRequest(final RegisterDataRequest request) {
        final RequestId originalRequestId = request.getId();
        LOGGER.debug("Registering data request: {}, {}", request, originalRequestId);

        final GetUserRequest userRequest = new GetUserRequest().userId(request.getUserId());
        userRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting user from userService: {}", userRequest);
        final User user = userService.getUser(userRequest);

        final GetResourcesByIdRequest resourceRequest = new GetResourcesByIdRequest().resourceId(request.getResourceId());
        LOGGER.debug("Getting resources from resourceService: {}", resourceRequest);
        final Map<LeafResource, ConnectionDetail> resources = resourceService.getResourcesById(resourceRequest);

        final RequestId requestId = new RequestId().id(request.getUserId().getId() + "-" + UUID.randomUUID().toString());

        final GetPolicyRequest policyRequest = new GetPolicyRequest().user(user).context(request.getContext()).resources(new HashSet<>(resources.keySet()));
        policyRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting policy from policyService: {}", request);
        MultiPolicy multiPolicy = policyService.getPolicy(policyRequest);

        return (CompletableFuture<DataRequestResponse>) aggregationService
                .aggregateDataRequestResults(request, user, resources, multiPolicy, requestId, originalRequestId);
    }

    @Override
    public CompletableFuture<DataRequestConfig> getDataRequestConfig(
            final GetDataRequestConfig request) {
        requireNonNull(request);
        requireNonNull(request.getId());
        // TODO: need to validate that the user is actually requesting the correct info.
        // extract resources from request and check they are a subset of the original RegisterDataRequest resources
        final GetCacheRequest<DataRequestConfig> cacheRequest = new GetCacheRequest<>().key(request.getId().getId()).service(this.getClass());
        LOGGER.debug("Getting cached data: {}", cacheRequest);
        return cacheService.get(cacheRequest)
                .thenApply(cache -> {
                    DataRequestConfig value = cache.orElseThrow(() -> createCacheException(request.getId().getId()));
                    if (null == value.getUser()) {
                        throw createCacheException(request.getId().getId());
                    }
                    LOGGER.debug("Got cache: {}", value);
                    return value;
                });
    }

    @Override
    public CompletableFuture<?> process(final Request request) {
        //first try one parent interface
        try {
            return PalisadeService.super.process(request);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private RuntimeException createCacheException(final String id) {
        return new RuntimeException(TOKEN_NOT_FOUND_MESSAGE + id);
    }

}
