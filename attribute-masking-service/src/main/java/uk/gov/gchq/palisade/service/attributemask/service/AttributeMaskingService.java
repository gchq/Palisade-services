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

package uk.gov.gchq.palisade.service.attributemask.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.repository.PersistenceLayer;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The attribute-masking-service is the final transformation the palisade system applies
 * to resources before they are returned.
 * The service performs two functions:
 * - Store the full details of the authorised request in a persistence store, to be later
 * retrieved by the data-service
 * - Mask the leafResource, removing any sensitive information - this may later include
 * applying a separate set of attributeRules, distinct from resourceRules and recordRules
 */
public class AttributeMaskingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMaskingService.class);
    private final PersistenceLayer persistenceLayer;
    private final LeafResourceMasker resourceMasker;

    /**
     * Constructor expected to be called by the ApplicationConfiguration, autowiring in the appropriate implementation of the repository (h2/redis/...)
     * as well as the appropriate masking function
     *
     * @param persistenceLayer the implementation of a PersistenceLayer to use
     * @param resourceMasker   the implementation of a LeafResourceMasker to use
     */
    public AttributeMaskingService(final PersistenceLayer persistenceLayer, final LeafResourceMasker resourceMasker) {
        this.persistenceLayer = persistenceLayer;
        this.resourceMasker = resourceMasker;
    }

    /**
     * Store the full details of the authorised request in a persistence store, to be later
     * retrieved by the data-service.
     *
     * @param token    the token {@link String} for the client request as a whole, created by the palisade-service
     * @param user     the {@link User} as authorised and returned by the user-service
     * @param resource one of many {@link LeafResource} as discovered and returned by the resource-service
     * @param context  the {@link Context} as originally supplied by the client
     * @param rules    the {@link Rules} that will be applied to the resource and its records as returned by the policy-service
     * @return a completable future representing the asynchronous completion of the storage operation
     */
    private CompletableFuture<AttributeMaskingRequest> storeAuthorisedRequest(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        LOGGER.info("Storing authorised request for token {} and leaf resource id {}", token, resource.getId());
        return this.persistenceLayer.putAsync(token, user, resource, context, rules);
    }

    /**
     * Given a nullable request, unwrap and store the request if it is non-null, ignore it if it is null
     *
     * @param token           the token for the client request as a whole
     * @param nullableRequest the request to the service
     * @return a completable future representing the asynchronous completion of the storage operation
     */
    @NonNull
    public CompletableFuture<AuditableAttributeMaskingRequest> storeAuthorisedRequest(final @NonNull String token, final @Nullable AttributeMaskingRequest nullableRequest) {
        return Optional.ofNullable(nullableRequest)
                .map((AttributeMaskingRequest request) -> {
                    try {
                        return storeAuthorisedRequest(token, request.getUser(), request.getResource(), request.getContext(), request.getRules())
                                .thenCompose(saved -> CompletableFuture.completedFuture(AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(request).withNoError()));
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.completedFuture(
                                AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(null)
                                        .withAuditErrorMessage(AuditErrorMessage.Builder.create().withUserId(request.getUserId())
                                                .withResourceId(request.getResourceId())
                                                .withContextNode(request.getContextNode())
                                                .withAttributes(Collections.singletonMap("method", "storeAuthorisedRequest"))
                                                .withError(e)));
                        }
                })
                .orElse(CompletableFuture.completedFuture(AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(null).withAuditErrorMessage(null)));
    }

    /**
     * Mask any sensitive attributes on a resource, possibly by applying attribute-level rules.
     *
     * @param user     the {@link User} as authorised and returned by the user-service
     * @param resource one of many {@link LeafResource} as discovered and returned by the resource-service, to be masked
     * @param context  the {@link Context} as originally supplied by the client
     * @param rules    the {@link Rules} that will be applied to the resource and its records as returned by the policy-service
     * @return a copy of the resource with sensitive data masked or redacted
     */
    private LeafResource maskResourceAttributes(final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        LOGGER.info("Masking resource attributes for leaf resource id {}", resource.getId());
        return resourceMasker.apply(resource);
    }

    /**
     * Given a nullable request, unwrap the request and mask the resource if it is non-null, ignore it if it is null
     *
     * @param nullableRequest the request to the service
     * @return a nullable response, with a masked resource if appropriate
     */
    @Nullable
    public AuditableAttributeMaskingResponse maskResourceAttributes(final @Nullable AttributeMaskingRequest nullableRequest) {
        return Optional.ofNullable(nullableRequest)
                .map((AttributeMaskingRequest request) -> {
                    try {
                        LeafResource maskedResource = maskResourceAttributes(request.getUser(), request.getResource(), request.getContext(), request.getRules());
                        return AuditableAttributeMaskingResponse.Builder.create()
                                .withAttributeMaskingResponse(AttributeMaskingResponse.Builder.create(request).withResource(maskedResource))
                                .withAuditErrorMessage(null);
                    } catch (JsonProcessingException e) {
                        return AuditableAttributeMaskingResponse.Builder.create()
                                .withAttributeMaskingResponse(null)
                                .withAuditErrorMessage(AuditErrorMessage.Builder.create()
                                    .withUserId(nullableRequest.getUserId())
                                    .withResourceId(nullableRequest.getResourceId())
                                    .withContextNode(nullableRequest.getContextNode())
                                    .withAttributes(Collections.singletonMap("method", "maskResourceAttributes"))
                                    .withError(e));
                    }
                })
                .orElse(AuditableAttributeMaskingResponse.Builder.create().withAttributeMaskingResponse(null).withAuditErrorMessage(null));
    }
}
