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
import uk.gov.gchq.palisade.service.palisade.request.GetCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.time.Duration;
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
    //Cache keys
    public static final String RES_COUNT_KEY = "res_count_";
    /**
     * Duration for how long the count of resources requested should live in the cache.
     */
    public static final Duration COUNT_PERSIST_DURATION = Duration.ofMinutes(10);
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeService.class);
    private final AuditService auditService;
    private final PolicyService policyService;
    private final UserService userService;
    private final ResourceService resourceService;
    private final CacheService cacheService;
    private final ResultAggregationService aggregationService;

    private final Executor executor;

    public SimplePalisadeService(final AuditService auditService, final UserService userService, final PolicyService policyService, final ResourceService resourceService,
                                 final CacheService cacheService, final Executor executor, final ResultAggregationService resultAggregationService) {
        requireNonNull(auditService, "auditService");
        requireNonNull(userService, "userService");
        requireNonNull(policyService, "policyService");
        requireNonNull(resourceService, "resourceService");
        requireNonNull(cacheService, "cacheService");
        requireNonNull(executor, "executor");
        requireNonNull(resultAggregationService, "resultAggregationService");
        this.auditService = auditService;
        this.userService = userService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.cacheService = cacheService;
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
        final GetPolicyRequest policyRequest = new GetPolicyRequest().user(user.join()).context(request.getContext()).resources(resources.join().keySet());
        policyRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting policy from policyService: {}", request);
        CompletableFuture<MultiPolicy> multiPolicy = policyService.getPolicy(policyRequest);

        LOGGER.debug("Aggregating results for \nrequest: {}, \nuser: {}, \nresources: {}, \npolicy:{}, \nrequestID: {}, \noriginal requestID: {}", request, user.join(), resources.join(), multiPolicy.join(), requestId, originalRequestId);
        CompletableFuture<DataRequestResponse> aggregatedResponse = aggregationService.aggregateDataRequestResults(
                request, user.join(), resources.join(), multiPolicy.join(), requestId, originalRequestId).toCompletableFuture();

        return aggregatedResponse;
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
                })
                .exceptionally(exception -> {
                    throw createCacheException(request.getId().getId());
                });
    }

    private RuntimeException createCacheException(final String id) {
        return new RuntimeException(TOKEN_NOT_FOUND_MESSAGE + id);
    }

}
