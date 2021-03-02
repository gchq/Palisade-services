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
import java.util.concurrent.Executor;

/**
 * Java JPA implementation of a {@link TokenErrorMessagePersistenceLayer} for the Filtered-Resource-Service.
 * Persist and retrieve {@link AuditErrorMessage#getError()} for a given request token.
 */
public class JpaTokenErrorMessagePersistenceLayer implements TokenErrorMessagePersistenceLayer {
    private final TokenErrorMessageRepository repository;
    private final Executor executor;

    /**
     * Construct a new Jpa-style persistence layer from a CrudRepository and async executor.
     * This simply wraps the repository methods with {@link CompletableFuture}s
     *
     * @param repository the CrudRepository of tokens and their associated exceptions
     * @param executor   an async executor to run the futures with
     */
    public JpaTokenErrorMessagePersistenceLayer(final TokenErrorMessageRepository repository, final Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<TokenErrorMessageEntity> putAuditErrorMessage(final String token, final String resourceId, final String userId, final Context context,
                                                                           final String serviceName, final Map<String, String> attributes, final Throwable error) {
        return CompletableFuture.supplyAsync(() -> repository.save(token, resourceId, userId, context, serviceName, attributes, error), executor);
    }

    @Override
    public CompletableFuture<List<TokenErrorMessageEntity>> getAllAuditErrorMessages(final String token) {
        return CompletableFuture.supplyAsync(() -> repository.findAllByToken(token), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAll(final List<TokenErrorMessageEntity> messageEntityList) {
        return CompletableFuture.runAsync(() -> repository.deleteAll(messageEntityList), executor);
    }

}
