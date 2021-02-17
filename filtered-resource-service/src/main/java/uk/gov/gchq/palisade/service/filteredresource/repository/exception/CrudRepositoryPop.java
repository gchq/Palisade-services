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

import akka.Done;
import akka.kafka.ConsumerMessage.Committable;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The CrudRepositoryPop is used to call the repository and asynchronously delete tokenAuditErrorMessageEntities
 */
public class CrudRepositoryPop implements Committable {
    private final Function<TokenAuditErrorMessageEntity, CompletableFuture<Void>> asyncDelete;
    private final TokenAuditErrorMessageEntity entity;

    /**
     * The default constructor of the CrudRepositoryPop, used for asynchronously deleting entities from the backing store
     *
     * @param entity      the {@link TokenAuditErrorMessageEntity} containing the {@link AuditErrorMessage} and token
     * @param asyncDelete the asyncDelete function used in the tokenAuditErrorPersistenceLayer to delete entities from the backing store
     */
    public CrudRepositoryPop(final Function<TokenAuditErrorMessageEntity, CompletableFuture<Void>> asyncDelete, final TokenAuditErrorMessageEntity entity) {
        this.asyncDelete = asyncDelete;
        this.entity = entity;
    }

    @Override
    public Future<Done> commitScaladsl() {
        return commitInternal();
    }

    @Override
    public CompletionStage<Done> commitJavadsl() {
        return this.asyncDelete.apply(entity).thenApply(ignored -> Done.done());
    }

    @Override
    public Future<Done> commitInternal() {
        return FutureConverters.toScala(commitJavadsl());
    }

    @Override
    public long batchSize() {
        return 1;
    }

}
