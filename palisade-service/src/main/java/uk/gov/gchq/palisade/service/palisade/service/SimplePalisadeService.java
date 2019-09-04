/*
 * Copyright 2018 Crown Copyright
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
import uk.gov.gchq.palisade.service.palisade.metrics.PalisadeMetricProvider;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * <p> A simple implementation of a Palisade Service that just connects up the Audit, Cache, User, Policy and Resource
 * services. </p> <p> It currently doesn't validate that the user is actually requesting the correct resources. It
 * should check the resources requested in getDataRequestConfig are the same or a subset of the resources passed in in
 * registerDataRequest. </p>
 */
public class SimplePalisadeService implements PalisadeService, PalisadeMetricProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeService.class);

    /**
     * Duration for how long the count of resources requested should live in the cache.
     */
    public static final Duration COUNT_PERSIST_DURATION = Duration.ofMinutes(10);

    @Override
    public CompletableFuture<DataRequestResponse> registerDataRequest(final RegisterDataRequest request) {
        final RequestId originalRequestId = new RequestId().id(UUID.randomUUID().toString());
        LOGGER.debug("Registering data request: {}", request, originalRequestId);
        auditRequestReceived(request, originalRequestId);
        final GetUserRequest userRequest = new GetUserRequest().userId(request.getUserId());
        userRequest.setOriginalRequestId(originalRequestId);
        LOGGER.debug("Getting user from userService: {}", userRequest);

        final CompletableFuture<User> futureUser = userService.getUser(userRequest)
                .thenApply(user -> {
                    LOGGER.debug("Got user: {}", user);
                    return user;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to get user: {}", ex.getMessage());
                    if (nonNull(ex)) {
                        auditRequestReceivedException(request, originalRequestId, ex);
                    }
                    throw new RuntimeException(ex); //rethrow the exception
                });

        final GetResourcesByIdRequest resourceRequest = new GetResourcesByIdRequest().resourceId(request.getResourceId());
        resourceRequest.setOriginalRequestId(originalRequestId);

        LOGGER.debug("Getting resources from resourceService: {}", resourceRequest);
        final CompletableFuture<Map<LeafResource, ConnectionDetail>> futureResources = resourceService.getResourcesById(resourceRequest)
                .thenApply(resources -> {
                    LOGGER.debug("Got resources: {}", resources);
                    return resources;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to get resources: {}", ex.getMessage());
                    if (nonNull(ex)) {
                        auditRequestReceivedException(request, originalRequestId, ex);
                    }
                    throw new RuntimeException(ex); //rethrow the exception
                });

        final RequestId requestId = new RequestId().id(request.getUserId().getId() + "-" + UUID.randomUUID().toString());

        final DataRequestConfig config = new DataRequestConfig();
        config.setContext(request.getContext());
        config.setOriginalRequestId(originalRequestId);

        CompletableFuture<MultiPolicy> futureMultiPolicy = getPolicy(request, futureUser, futureResources, originalRequestId);

        return CompletableFuture.allOf(futureUser, futureResources, futureMultiPolicy)
                .thenApply(t -> {
                    //remove any resources from the map that the policy doesn't contain details for -> user should not even be told about
                    //resources they don't have permission to see
                    Map<LeafResource, ConnectionDetail> filteredResources = removeDisallowedResources(futureResources.join(), futureMultiPolicy.join());

                    PalisadeService.ensureRecordRulesAvailableFor(futureMultiPolicy.join(), filteredResources.keySet());
                    auditProcessingStarted(request, futureUser.join(), futureMultiPolicy.join(), originalRequestId);
                    cache(request, futureUser.join(), requestId, futureMultiPolicy.join(), filteredResources.size(), originalRequestId);

                    final DataRequestResponse response = new DataRequestResponse().requestId(requestId).originalRequestId(originalRequestId).resources(filteredResources);
                    response.setOriginalRequestId(originalRequestId);
                    LOGGER.debug("Responding with: {}", response);
                    return response;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Error handling: {}", ex.getMessage());
                    if (nonNull(ex)) {
                        auditRequestReceivedException(request, originalRequestId, ex);

                    }
                    throw new RuntimeException(ex); //rethrow the exception
                });
    }

}
