/*
 * Copyright 2020 Crown Copyright
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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.palisade.exception.RegisterRequestException;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
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

    /**
     * Service name
     */
    public static final String NAME = "palisade-service";

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

    private static AuditRequest.RegisterRequestExceptionAuditRequest.IException getAuditException(final RegisterDataRequest request) {
        return AuditRequest.RegisterRequestExceptionAuditRequest
                .create(request.getId())
                .withUserId(request.getUserId())
                .withResourceId(request.getResourceId())
                .withContext(request.getContext());
    }

    private CompletableFuture<User> getFutureUser(final RegisterDataRequest request) {
        final GetUserRequest userRequest = new GetUserRequest().userId(request.getUserId());
        userRequest.setOriginalRequestId(request.getId());
        return userService
                .getUser(userRequest)
                .exceptionally((Throwable ex) -> {
                    auditRequestReceivedException(request, ex, UserService.NAME);
                    throw new RegisterRequestException("Exception from userService", ex);
                });
    }

    private CompletableFuture<Set<LeafResource>> getFutureResources(final RegisterDataRequest request) {
        final GetResourcesByIdRequest resourceRequest = new GetResourcesByIdRequest().resourceId(request.getResourceId());
        resourceRequest.setOriginalRequestId(request.getId());
        return resourceService
                .getResourcesById(resourceRequest)
                .exceptionally((Throwable ex) -> {
                    auditRequestReceivedException(request, ex, ResourceService.NAME);
                    throw new RegisterRequestException("Exception from resourceService", ex);
                });
    }

    private CompletableFuture<Map<LeafResource, Rules>> getFutureResourceRules(
            final RegisterDataRequest request,
            final CompletableFuture<User> futureUser,
            final CompletableFuture<Set<LeafResource>> futureResources) {

        return futureUser.thenCombine(futureResources, (User user, Collection<LeafResource> resources) -> {
            final GetPolicyRequest policyRequest = new GetPolicyRequest()
                    .user(user)
                    .context(request.getContext())
                    .resources(resources);
            policyRequest.setOriginalRequestId(request.getId());
            return policyService.getPolicy(policyRequest).join();
        }).exceptionally((Throwable ex) -> {
            auditRequestReceivedException(request, ex, PolicyService.NAME);
            throw new RegisterRequestException("Exception from policyService", ex);
        });
    }

    @Override
    public DataRequestResponse registerDataRequest(final RegisterDataRequest request) {
        requireNonNull(request, "request");
        try {
            LOGGER.debug("Registering data request: {}", request);
            LOGGER.info("Registering data request: {}", request.getId());

            CompletableFuture<User> futureUser = getFutureUser(request);
            CompletableFuture<Set<LeafResource>> futureResources = getFutureResources(request);
            CompletableFuture<Map<LeafResource, Rules>> futureRules = getFutureResourceRules(request, futureUser, futureResources);

            final String token = request.getUserId().getId() + "-" + UUID.randomUUID().toString();
            LOGGER.info("Assigned token {} for request {}", token, request.getId());
            CompletableFuture<DataRequestResponse> futureResponse = CompletableFuture.supplyAsync(
                    () -> aggregationService.aggregateDataRequestResults(
                            request,
                            futureUser,
                            futureResources,
                            futureRules,
                            token),
                    executor);

            DataRequestResponse response = futureResponse.join();
            auditRequestComplete(request, futureUser.join(), futureResources.join(), request.getContext());

            return response;
        } catch (RuntimeException ex) {
            auditRequestReceivedException(request, ex, SimplePalisadeService.NAME);
            throw new RegisterRequestException(ex);
        } catch (Error err) {
            // Either an auditRequestComplete or auditRequestException MUST be called here, so catch a broader set of Exception classes than might be expected
            // Generally this is a bad idea, but we need guarantees of the audit - ie. malicious attempt at StackOverflowError
            auditRequestReceivedException(request, err, SimplePalisadeService.NAME);
            // Rethrow this Error, don't wrap it in the RegisterRequestException
            throw err;
        }
    }

    private void auditRequestComplete(final RegisterDataRequest request, final User user, final Set<LeafResource> resources, final Context context) {
        LOGGER.info("Auditing completed register request with audit service");
        auditService.audit(AuditRequest.RegisterRequestCompleteAuditRequest.create(request.getId())
                .withUser(user)
                .withLeafResources(resources)
                .withContext(context));
    }

    private void auditRequestReceivedException(final RegisterDataRequest request, final Throwable ex, final String serviceName) {
        LOGGER.error("Error while handling request: {}", request);
        LOGGER.error("Exception was", ex);
        LOGGER.info("Auditing error with audit service");
        AuditRequest auditException = getAuditException(request)
                .withException(ex)
                .withServiceName(serviceName);
        auditService.audit(auditException);
    }

    @Override
    public DataRequestConfig getDataRequestConfig(final GetDataRequestConfig request) {
        requireNonNull(request);
        requireNonNull(request.getToken());
        // extract resources from request and check they are a subset of the original RegisterDataRequest resources
        LOGGER.debug("Getting cached data: {}", request);

        return this.persistenceLayer.get(request.getOriginalRequestId().getId());
    }

}
