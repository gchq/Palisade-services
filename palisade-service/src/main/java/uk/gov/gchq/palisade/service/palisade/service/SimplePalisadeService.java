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
import uk.gov.gchq.palisade.service.palisade.metrics.PalisadeMetricProvider;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.request.AddCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest.RegisterRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetCacheRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetMetricRequest;
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
import java.util.Optional;
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
public class SimplePalisadeService implements PalisadeService, PalisadeMetricProvider {
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

    private final Executor executor;

    public SimplePalisadeService(final AuditService auditService, final UserService userService, final PolicyService policyService, final ResourceService resourceService, final CacheService cacheService, final Executor executor) {
        this.auditService = auditService;
        this.userService = userService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.cacheService = cacheService;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<DataRequestResponse> registerDataRequest(final RegisterDataRequest request) {
        final RequestId originalRequestId = request.getOriginalRequestId();
        LOGGER.debug("Registering data request: {}", request, originalRequestId);
        final GetUserRequest userRequest = new GetUserRequest().userId(request.getUserId());
        userRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting user from userService: {}", userRequest);

        final CompletionStage<User> futureUser = userService.getUser(userRequest)
                .thenApply(user -> {
                    LOGGER.debug("Got user: {}", user);
                    return user;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to get user: {}", ex.getMessage());
                    auditRequestReceivedException(request, ex, UserService.class);
                    throw new RuntimeException(ex); //rethrow the exception
                });

        final GetResourcesByIdRequest resourceRequest = new GetResourcesByIdRequest().resourceId(request.getResourceId());
        LOGGER.debug("Getting resources from resourceService: {}", resourceRequest);

        final CompletionStage<Map<LeafResource, ConnectionDetail>> futureResources = resourceService.getResourcesById(resourceRequest)
                .thenApply(resources -> {
                    LOGGER.debug("Got resources: {}", resources);
                    return resources;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to get resources: {}", ex.getMessage());
                    auditRequestReceivedException(request, ex, ResourceService.class);
                    throw new RuntimeException(ex); //rethrow the exception
                });

        final RequestId requestId = new RequestId().id(request.getUserId().getId() + "-" + UUID.randomUUID().toString());

        CompletionStage<MultiPolicy> futureMultiPolicy = getPolicy(request, futureUser, futureResources, originalRequestId);

        return CompletableFuture.allOf(futureUser.toCompletableFuture(), futureResources.toCompletableFuture(), futureMultiPolicy.toCompletableFuture())
                .thenApply(t -> {
                    //remove any resources from the map that the policy doesn't contain details for -> user should not even be told about
                    //resources they don't have permission to see
                    Map<LeafResource, ConnectionDetail> filteredResources = removeDisallowedResources(futureResources.toCompletableFuture().join(), futureMultiPolicy.toCompletableFuture().join());

                    PalisadeService.ensureRecordRulesAvailableFor(futureMultiPolicy.toCompletableFuture().join(), filteredResources.keySet());
                    auditRegisterRequestComplete(request, futureUser.toCompletableFuture().join(), futureMultiPolicy.toCompletableFuture().join());
                    cache(request, futureUser.toCompletableFuture().join(), requestId, futureMultiPolicy.toCompletableFuture().join(), filteredResources.size(), originalRequestId); // *********

                    final DataRequestResponse response = new DataRequestResponse().requestId(requestId.id(request.getId())).resources(filteredResources);
                    response.setOriginalRequestId(originalRequestId);
                    LOGGER.debug("Responding with: {}", response);
                    return response;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Error handling: {}", ex.getMessage());
                    auditRequestReceivedException(request, ex, PolicyService.class);
                    throw new RuntimeException(ex); //rethrow the exception
                });
    }

    /**
     * Removes all resource mappings in the {@code resources} that do not have a defined policy in {@code policy}.
     *
     * @param resources the resources to modify
     * @param policy    the policy for all resources
     * @return the {@code resources} map after filtering
     */
    private Map<LeafResource, ConnectionDetail> removeDisallowedResources(final Map<LeafResource, ConnectionDetail> resources, final MultiPolicy policy) {
        resources.keySet().retainAll(policy.getPolicies().keySet());
        return resources;
    }

    private CompletableFuture<MultiPolicy> getPolicy(final RegisterDataRequest request, final CompletionStage<User> futureUser, final CompletionStage<Map<LeafResource, ConnectionDetail>> futureResources, final RequestId originalRequestId) {
        return CompletableFuture.allOf(futureUser.toCompletableFuture(), futureResources.toCompletableFuture())
                .thenApply(t -> {
                    final GetPolicyRequest policyRequest = new GetPolicyRequest()
                            .user(futureUser.toCompletableFuture().join())
                            .context(request.getContext())
                            .resources(new HashSet<>(futureResources.toCompletableFuture().join().keySet()));
                    policyRequest.setOriginalRequestId(originalRequestId);
                    return policyRequest;
                })
                .thenApply(req -> {
                    LOGGER.debug("Getting policy from policyService: {}", req);
                    return policyService.getPolicy(req).toCompletableFuture().join();
                }).thenApply(policy -> {
                    LOGGER.debug("Got policy: {}", policy);
                    return policy;
                });
    }

    private void auditRegisterRequestComplete(final RegisterDataRequest request, final User user, final MultiPolicy multiPolicy) {
        RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest = RegisterRequestCompleteAuditRequest.create(request.getOriginalRequestId())
                .withUser(user)
                .withLeafResources(multiPolicy.getPolicies().keySet())
                .withContext(request.getContext());
        LOGGER.debug("Auditing: {}", registerRequestCompleteAuditRequest);
        auditService.audit(registerRequestCompleteAuditRequest).toCompletableFuture().join();
    }

    private void auditRequestReceivedException(final RegisterDataRequest request, final Throwable ex, final Class<? extends Service> serviceClass) {
        final RegisterRequestExceptionAuditRequest auditRequestWithException =
                RegisterRequestExceptionAuditRequest.create(request.getOriginalRequestId())
                .withUserId(request.getUserId())
                .withResourceId(request.getResourceId())
                .withContext(request.getContext())
                .withException(ex)
                .withServiceClass(serviceClass);
        LOGGER.debug("Error handling: " + ex.getMessage());
        auditService.audit(auditRequestWithException).toCompletableFuture().join();
    }

    private void cache(final RegisterDataRequest request, final User user,
                       final RequestId requestId, final MultiPolicy multiPolicy,
                       final int resCount,
                       final RequestId originalRequestId) {
        DataRequestConfig dataRequestConfig = new DataRequestConfig()
                .user(user)
                .context(request.getContext())
                .rules(multiPolicy.getRuleMap());
        dataRequestConfig.setOriginalRequestId(originalRequestId);
        final AddCacheRequest<DataRequestConfig> cacheRequest = new AddCacheRequest<>()
                .key(requestId.getId())
                .value(dataRequestConfig)
                .service(this.getClass());
        LOGGER.debug("Caching: {}", cacheRequest);
        final Boolean success = cacheService.add(cacheRequest).join();
        if (null == success || !success) {
            throw new CompletionException(new RuntimeException("Failed to cache request: " + request));
        }
        //set a quick count of how many resources are needed for this request
        final AddCacheRequest<byte[]> resourceCountRequest = new AddCacheRequest<>()
                .key(RES_COUNT_KEY + requestId.getId() + "_" + resCount)
                .value(new byte[1])
                .timeToLive(Optional.of(COUNT_PERSIST_DURATION))
                .service(this.getClass());
        cacheService.add(resourceCountRequest).join();
    }

    @Override
    public CompletableFuture<DataRequestConfig> getDataRequestConfig(
            final GetDataRequestConfig request) {
        requireNonNull(request);
        requireNonNull(request.getId());
        // TODO: need to validate that the user is actually requesting the correct info.
        // extract resources from request and check they are a subset of the original RegisterDataRequest resources
        final GetCacheRequest<DataRequestConfig> cacheRequest = new GetCacheRequest<>().key(request.getId()).service(this.getClass());
        LOGGER.debug("Getting cached data: {}", cacheRequest);
        return cacheService.get(cacheRequest)
                .thenApply(cache -> {
                    DataRequestConfig value = cache.orElseThrow(() -> createCacheException(request.getId()));
                    if (null == value.getUser()) {
                        throw createCacheException(request.getId());
                    }
                    LOGGER.debug("Got cache: {}", value);
                    return value;
                });
    }

    @Override
    public CompletableFuture<Map<String, String>> getMetrics(final GetMetricRequest request) {
        requireNonNull(request, "request");
        return null;
    }

    @Override
    public CompletableFuture<?> process(final Request request) {
        //first try one parent interface
        try {
            return PalisadeService.super.process(request);
        } catch (IllegalArgumentException e) {
            //that failed try the other
            return PalisadeMetricProvider.super.process(request);
        }
    }

    private RuntimeException createCacheException(final String id) {
        return new RuntimeException(TOKEN_NOT_FOUND_MESSAGE + id);
    }

}
