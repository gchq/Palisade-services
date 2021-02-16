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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class CrudRepositoryPop implements Committable {
    private final TokenAuditErrorMessageRepository repository;
    private final TokenAuditErrorMessageEntity entity;
    private final Executor executor;

    public CrudRepositoryPop(final TokenAuditErrorMessageRepository repository, final TokenAuditErrorMessageEntity entity, final Executor executor) {
        this.repository = repository;
        this.entity = entity;
        this.executor = executor;
    }

    @Override
    public Future<Done> commitScaladsl() {
        return commitInternal();
    }

    @Override
    public CompletionStage<Done> commitJavadsl() {
        return FutureConverters.toJava(commitInternal());
    }

    @Override
    public Future<Done> commitInternal() {
        return repository.asyncDelete(entity, executor);
    }

    @Override
    public long batchSize() {
        return 1;
    }

}
