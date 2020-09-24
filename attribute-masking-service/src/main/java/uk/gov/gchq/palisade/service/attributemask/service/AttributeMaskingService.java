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
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;

import java.io.IOException;
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
public interface AttributeMaskingService {

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
     * @throws IOException if a failure occurred writing to the persistence store.
     */
    CompletableFuture<Void> storeAuthorisedRequest(final @NonNull String token, final @NonNull User user, final @NonNull LeafResource resource, final @NonNull Context context, final @NonNull Rules<?> rules);

    default CompletableFuture<Void> storeAuthorisedRequest(final @NonNull String token, final @Nullable AttributeMaskingRequest nullableRequest) {
        return Optional.ofNullable(nullableRequest)
                .map(request -> {
                    try {
                        return storeAuthorisedRequest(token, request.getUser(), request.getResource(), request.getContext(), request.getRules());
                    } catch (JsonProcessingException e) {
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        failedFuture.obtrudeException(e);
                        return failedFuture;
                    }
                })
                .orElse(CompletableFuture.completedFuture(null));
    }

    /**
     * Mask any sensitive attributes on a resource, possibly by applying attribute-level rules.
     *
     * @param resource the (sensitive) resource to be returned to the client
     * @return a copy of the resource with sensitive data masked or redacted
     */
    LeafResource maskResourceAttributes(final User user, final LeafResource resource, final Context context, final Rules<?> rules);

    default AttributeMaskingResponse maskResourceAttributes(final @Nullable AttributeMaskingRequest nullableRequest) {
        return Optional.ofNullable(nullableRequest)
                .map(request -> {
                    try {
                        LeafResource maskedResource = maskResourceAttributes(request.getUser(), request.getResource(), request.getContext(), request.getRules());
                        return AttributeMaskingResponse.Builder.create(request).withResource(maskedResource);
                    } catch (JsonProcessingException e) {
                        throw new SerializationFailedException("Failed to deserialize request json members", e);
                    }
                })
                .orElse(null);
    }

}
