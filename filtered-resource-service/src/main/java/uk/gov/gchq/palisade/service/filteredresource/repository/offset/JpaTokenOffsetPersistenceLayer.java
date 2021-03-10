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
package uk.gov.gchq.palisade.service.filteredresource.repository.offset;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenOffsetEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Java JPA implementation of a {@link TokenOffsetPersistenceLayer} for the filtered-resource-service.
 * Persist and retrieve topic offsets for a given request token.
 */
public class JpaTokenOffsetPersistenceLayer implements TokenOffsetPersistenceLayer {
    private static final CompletableFuture<Void> DONE = CompletableFuture.completedFuture(null);

    private final TokenOffsetRepository repository;
    private final Executor executor;

    /**
     * Construct a new Jpa-style persistence layer from a CrudRepository and async executor.
     * This simply wraps the repository methods with {@link CompletableFuture}s
     *
     * @param repository the CrudRepository of tokens and their kafka offsets
     * @param executor   an async executor to run the futures with
     */
    public JpaTokenOffsetPersistenceLayer(final TokenOffsetRepository repository, final Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> putOffsetIfAbsent(final String token, final Long offset) {
        return findOffset(token)
                .thenComposeAsync((Optional<Long> existing) -> existing
                                .map(ignored -> DONE)
                                .orElseGet(() -> overwriteOffset(token, offset)),
                        executor);
    }

    @Override
    public CompletableFuture<Void> overwriteOffset(final String token, final Long offset) {
        return CompletableFuture.runAsync(() -> repository
                .save(token, offset), executor);
    }

    @Override
    public CompletableFuture<Optional<Long>> findOffset(final String token) {
        return CompletableFuture.supplyAsync(() -> repository
                .findByToken(token)
                .map(TokenOffsetEntity::getOffset));
    }
}
