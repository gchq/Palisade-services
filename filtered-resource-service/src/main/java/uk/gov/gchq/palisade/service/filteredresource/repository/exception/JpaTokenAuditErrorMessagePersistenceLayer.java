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

import akka.japi.Pair;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Java JPA implementation of a {@link TokenAuditErrorMessagePersistenceLayer} for the Filtered-Resource-Service.
 * Persist and retrieve {@link AuditErrorMessage#getError()} for a given request token.
 */
public class JpaTokenAuditErrorMessagePersistenceLayer implements TokenAuditErrorMessagePersistenceLayer {
    private final TokenAuditErrorMessageRepository repository;
    private final Executor executor;

    /**
     * Construct a new Jpa-style persistence layer from a CrudRepository and async executor.
     * This simply wraps the repository methods with {@link CompletableFuture}s
     *
     * @param repository the CrudRepository of tokens and their associated exceptions
     * @param executor   an async executor to run the futures with
     */
    public JpaTokenAuditErrorMessagePersistenceLayer(final TokenAuditErrorMessageRepository repository, final Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final AuditErrorMessage auditErrorMessage) {
        return CompletableFuture.supplyAsync(() -> repository.save(token, auditErrorMessage), executor);
    }

    @Override
    public CompletableFuture<Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>>> popAuditErrorMessage(final String token) {
        // Get the exception from the repository
        return CompletableFuture.supplyAsync(() -> repository.findFirstByToken(token), executor)
                // Then delete the exception from the repositry
                .thenApply((Optional<TokenAuditErrorMessageEntity> entityOptional) -> entityOptional.map(entity -> Pair.create(entity, new CrudRepositoryPop(repository, entity, executor))));
    }
}
