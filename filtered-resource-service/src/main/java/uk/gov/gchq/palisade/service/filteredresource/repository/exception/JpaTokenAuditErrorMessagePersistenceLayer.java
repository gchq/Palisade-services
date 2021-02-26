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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.List;
import java.util.Map;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTokenAuditErrorMessagePersistenceLayer.class);


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
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final String resourceId, final String userId, final Context context, final Map<String, String> attributes, final Throwable error) {
        LOGGER.info("JPATokenAuditErrorMessagePersistenceLayer put error is {}", error.getMessage());
        return CompletableFuture.supplyAsync(() -> repository.save(token, resourceId, userId, context, attributes, error), executor)
                .thenApply((TokenAuditErrorMessageEntity entity) -> {
                    LOGGER.info("entity was: {}", entity);
                    return entity;
                });
    }

    @Override
    public CompletableFuture<Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>>> popAuditErrorMessage(final String token) {
        // Get the exception from the repository
        return CompletableFuture.supplyAsync(() -> repository.findFirstByToken(token), executor)
                // Then delete the exception from the repositry
                .thenApply((Optional<TokenAuditErrorMessageEntity> entityOptional) -> entityOptional.map(entity -> Pair.create(entity, new CrudRepositoryPop(this::asyncDelete, entity))))
                .thenApply((Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>> repositoryPopPair) -> {
                    LOGGER.info("JPATokenAuditErrorMessagePersistenceLayer pop error is {}", repositoryPopPair.map(entity -> entity.first().toString()).orElse("empty"));
                    return repositoryPopPair;
                });
    }

    @Override
    public CompletableFuture<Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>>> popAuditErrorMessage(final TokenAuditErrorMessageEntity entity) {
        // Get the exception from the repository
        return CompletableFuture.supplyAsync(() -> repository.findByEntity(entity), executor)
                // Then delete the exception from the repositry
                .thenApply((Optional<TokenAuditErrorMessageEntity> entityOptional) -> entityOptional.map(messageEntity -> Pair.create(messageEntity, new CrudRepositoryPop(this::asyncDelete, messageEntity))))
                .thenApply((Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>> repositoryPopPair) -> {
                    LOGGER.info("JPATokenAuditErrorMessagePersistenceLayer pop error is {}", repositoryPopPair.map(tokenEntity -> tokenEntity.first().toString()).orElse("empty"));
                    return repositoryPopPair;
                });
    }

    @Override
    public CompletableFuture<Optional<List<TokenAuditErrorMessageEntity>>> getAllAuditErrorMessages(final String token) {
        return CompletableFuture.supplyAsync(() -> repository.findAllByToken(token), executor);
    }

    /**
     * Calls the {@link TokenAuditErrorMessageRepository#delete(Object)} method and deletes the entity from the backing store,
     * then returns a CompletableFuture when completed
     *
     * @param entity the {@link TokenAuditErrorMessageEntity} containing the {@link AuditErrorMessage} and token
     * @return a CompletableFuture#void after the async call has completed
     */
    public CompletableFuture<Void> asyncDelete(final TokenAuditErrorMessageEntity entity) {
        return CompletableFuture.runAsync(() -> repository.delete(entity), executor);
    }
}
