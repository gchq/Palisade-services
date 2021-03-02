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

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.WorkerCommand;

import java.util.List;

final class TokenAuditErrorMessageWorker extends AbstractBehavior<WorkerCommand> {

    protected interface WorkerCommand {
        // Marker interface for inputs of the worker
    }

    protected interface WorkerResponse {
        // Marker interface for outputs of the worker
    }

    protected static class GetAllExceptions implements WorkerCommand {
        protected final String token;
        protected final ActorRef<WorkerResponse> replyTo;

        protected GetAllExceptions(final String token, final ActorRef<WorkerResponse> replyTo) {
            this.token = token;
            this.replyTo = replyTo;
        }
    }

    protected static class SetAuditErrorMessages implements WorkerCommand, WorkerResponse {
        protected final String token;
        protected final List<TokenAuditErrorMessageEntity> messageEntities;

        protected SetAuditErrorMessages(final String token, final List<TokenAuditErrorMessageEntity> messageEntities) {
            this.token = token;
            this.messageEntities = messageEntities;
        }
    }

    protected static class ReportError implements WorkerResponse {
        protected final String token;
        protected final Throwable exception;

        protected ReportError(final String token, final Throwable exception) {
            this.token = token;
            this.exception = exception;
        }
    }

    private final TokenAuditErrorMessagePersistenceLayer persistenceLayer;

    private TokenAuditErrorMessageWorker(final ActorContext<WorkerCommand> context,
                                         final TokenAuditErrorMessagePersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    static Behavior<WorkerCommand> create(final TokenAuditErrorMessagePersistenceLayer persistenceLayer) {
        return Behaviors.setup(ctx -> new TokenAuditErrorMessageWorker(ctx, persistenceLayer));
    }

    @Override
    public Receive<WorkerCommand> createReceive() {
        return this.onGetExceptions();
    }

    private Receive<WorkerCommand> onGetExceptions() {
        return newReceiveBuilder()

                // Start off in Getting mode
                .onMessage(GetAllExceptions.class, (GetAllExceptions getCmd) -> this.persistenceLayer
                        // Get from persistence
                        .getAllAuditErrorMessages(getCmd.token)
                        // If present tell self (if not, will be told in the future)
                        .thenApply((List<TokenAuditErrorMessageEntity> listAEM) -> {
                            getCmd.replyTo.tell(new SetAuditErrorMessages(getCmd.token, listAEM));
                            return listAEM;
                        })
                        .thenCompose(this.persistenceLayer::deleteAll)
                        .<Behavior<WorkerCommand>>thenApply(ignored -> Behaviors.stopped())

                        // If an exception is thrown reading from persistence, report the exception
                        .exceptionally((Throwable ex) -> {
                            getCmd.replyTo.tell(new ReportError(getCmd.token, ex));
                            return Behaviors.stopped();
                        })
                        .join())
                .build();
    }
}
