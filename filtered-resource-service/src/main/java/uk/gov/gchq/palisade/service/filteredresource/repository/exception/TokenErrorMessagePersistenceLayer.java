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
package uk.gov.gchq.palisade.service.filteredresource.repository.exception;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Persist and retrieve {@link TokenErrorMessageEntity} for a given request token.
 */
public interface TokenErrorMessagePersistenceLayer {

    /**
     * Save the token and variabled from a {@link AuditErrorMessage} in the persistence
     *
     * @param token       the unique token from the request to the Palisade Service
     * @param userId      the id of the user associated with the request
     * @param resourceId  the id of the resource associated with the request
     * @param context     the context associated with the request
     * @param serviceName the name of the service that the error was thrown from
     * @param attributes  any additional attributes about the request
     * @param error       the error thrown in the service
     * @return a {@link CompletableFuture} of a {@link TokenErrorMessageEntity} containing the values just persisted
     */
    CompletableFuture<TokenErrorMessageEntity> putAuditErrorMessage(final String token, String userId, String resourceId, Context context, String serviceName, Map<String, String> attributes, Throwable error);

    /**
     * Gets all AuditErrorMessages that are linked to the unique request token, and packages them in a List of {@link TokenErrorMessageEntity}(s)
     *
     * @param token the unique token from the request
     * @return a list of all errors associated with this token from other services, packaged in a {@link TokenErrorMessageEntity}
     */
    CompletableFuture<List<TokenErrorMessageEntity>> getAllAuditErrorMessages(final String token);

    /**
     * Deletes the token and AuditErrorMessages that have been previously retrieved from persistence.
     * It will only delete items that are in the list, for example, late errors that are persisted after you have called {@link #getAllAuditErrorMessages(String)} will not be deleted
     *
     * @param messageEntityList a list of previously retrieved AuditErrorMessages and their unique token
     * @return a future completing once all tokens and relating AuditErrorMessages have been deleted from persistence
     */
    CompletableFuture<Void> deleteAll(List<TokenErrorMessageEntity> messageEntityList);
}