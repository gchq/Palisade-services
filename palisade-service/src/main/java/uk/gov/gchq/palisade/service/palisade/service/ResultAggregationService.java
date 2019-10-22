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
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class ResultAggregationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultAggregationService.class);
    private AuditService auditService;
    private CacheService cacheService;

    public ResultAggregationService(final AuditService auditService, final CacheService cacheService) {
        this.auditService = auditService;
        this.cacheService = cacheService;
    }

    public CompletionStage<DataRequestResponse> aggregateDataRequestResults(final RegisterDataRequest request,
                                                                            final User user,
                                                                            final Map<LeafResource, ConnectionDetail> resource,
                                                                            final MultiPolicy policy,
                                                                            final RequestId requestId,
                                                                            final RequestId originalRequestId) {

        try {
            //remove any resources from the map that the policy doesn't contain details for -> user should not even be told about
            //resources they don't have permission to see
            Map<LeafResource, ConnectionDetail> filteredResources = removeDisallowedResources(resource, policy);

            PalisadeService.ensureRecordRulesAvailableFor(policy, filteredResources.keySet());
            auditRegisterRequestComplete(request, user, policy, auditService);

            cache(cacheService, request, user, requestId, policy, filteredResources.size(), originalRequestId);

            final DataRequestResponse response = new DataRequestResponse().resources(filteredResources);
            response.setOriginalRequestId(originalRequestId);
            LOGGER.debug("Responding with: {}", response);

            return CompletableFuture.completedStage(response);
        } catch (Exception ex) {
            LOGGER.error("Error handling: {}", ex.getMessage());

            auditRequestReceivedException(request, ex, PolicyService.class, auditService);
            throw new RuntimeException(ex); //rethrow the exception
        }
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

    private void auditRegisterRequestComplete(final RegisterDataRequest request, final User user, final MultiPolicy multiPolicy, final AuditService auditService) {
        RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest = RegisterRequestCompleteAuditRequest.create(request.getId())
                .withUser(user)
                .withLeafResources(multiPolicy.getPolicies().keySet())
                .withContext(request.getContext());
        LOGGER.debug("Auditing: {}", registerRequestCompleteAuditRequest);
        auditService.audit(registerRequestCompleteAuditRequest);
    }

    private void auditRequestReceivedException(final RegisterDataRequest request, final Throwable ex, final Class<? extends Service> serviceClass, final AuditService auditService) {
        final RegisterRequestExceptionAuditRequest auditRequestWithException =
                RegisterRequestExceptionAuditRequest.create(request.getId())
                        .withUserId(request.getUserId())
                        .withResourceId(request.getResourceId())
                        .withContext(request.getContext())
                        .withException(ex)
                        .withServiceClass(serviceClass);
        LOGGER.debug("Error handling: " + ex.getMessage());
        auditService.audit(auditRequestWithException);
    }

    private void cache(final CacheService cacheService,
                       final RegisterDataRequest request, final User user,
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
    }
}
