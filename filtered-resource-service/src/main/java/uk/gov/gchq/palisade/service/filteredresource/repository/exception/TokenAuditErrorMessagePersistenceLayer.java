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

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persist and retrieve {@link AuditErrorMessage} for a given request token.
 */
public interface TokenAuditErrorMessagePersistenceLayer {

    /**
     * Save the token and AuditErrorMessage in the persistence
     *
     * @param token             the unique token
     * @param auditErrorMessage the {@link AuditErrorMessage} associated with this token
     * @return a {@link CompletableFuture} of a {@link TokenAuditErrorMessageEntity} containing the value just persisted
     */
    CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, AuditErrorMessage auditErrorMessage);

    /**
     * Get it, delete from repository and return to client
     *
     * @param token the unique request token
     * @return an optional.empty if there are no AuditErrorMessages left, or the throwable exception from the repository
     */
    CompletableFuture<Optional<AuditErrorMessage>> popAuditErrorMessage(final String token);
}
