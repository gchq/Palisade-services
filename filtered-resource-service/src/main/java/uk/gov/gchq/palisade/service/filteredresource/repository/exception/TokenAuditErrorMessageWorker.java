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
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageController.DeregisterWorker;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.WorkerCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TokenAuditErrorMessageWorker extends AbstractBehavior<WorkerCommand> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuditErrorMessageWorker.class);

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

    protected static class SetAllExceptions implements WorkerCommand, WorkerResponse {
        protected final String token;
        protected final ArrayList<AuditErrorMessage> auditErrorMessage = new ArrayList<>();

        protected SetAllExceptions(final String token, final AuditErrorMessage auditErrorMessage) {
            this.token = token;
            this.auditErrorMessage.add(auditErrorMessage);
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
    private final ActorRef<DeregisterWorker> parent;

    private TokenAuditErrorMessageWorker(final ActorContext<WorkerCommand> context,
                                         final TokenAuditErrorMessagePersistenceLayer persistenceLayer,
                                         final ActorRef<DeregisterWorker> parent) {
        super(context);
        this.persistenceLayer = persistenceLayer;
        this.parent = parent;
    }

    static Behavior<WorkerCommand> create(final TokenAuditErrorMessagePersistenceLayer persistenceLayer, final ActorRef<DeregisterWorker> parent) {
        return Behaviors.setup(ctx -> new TokenAuditErrorMessageWorker(ctx, persistenceLayer, parent));
    }

    @Override
    public Receive<WorkerCommand> createReceive() {
        return this.onGetExceptions();
    }

    private Receive<WorkerCommand> onGetExceptions() {
        return newReceiveBuilder()
                // Deregister self with parent on stop
                .onSignal(PostStop.class, this::onPostStop)

                // Start off in Getting mode
                .onMessage(GetAllExceptions.class, (GetAllExceptions getCmd) -> this.persistenceLayer
                        // Get from persistence
                        .getAllAuditErrorMessages(getCmd.token)
                        // If present tell self (if not, will be told in the future)
                        .<Behavior<WorkerCommand>>thenApply((Optional<List<TokenAuditErrorMessageEntity>> messageEntities) -> {
                            messageEntities.ifPresent(entityList -> entityList.forEach((TokenAuditErrorMessageEntity entity) -> {
                                this.getContext().getSelf().tell(new SetAllExceptions(getCmd.token, entity.getAuditErrorMessage()));
                                //Finally remove the entity from the persistence layer
                                this.persistenceLayer.popAuditErrorMessage(entity);
                            }));
                            return this.onSetException(getCmd);
                        })
                        // If an exception is thrown reading from persistence, report the exception
                        .exceptionally((Throwable ex) -> {
                            getCmd.replyTo.tell(new ReportError(getCmd.token, ex));
                            return Behaviors.stopped();
                        })
                        .join())
                .build();
    }

    private Receive<WorkerCommand> onSetException(final GetAllExceptions getCmd) {
        return newReceiveBuilder()
                // Deregister self with parent on stop
                .onSignal(PostStop.class, this::onPostStop)

                // Switch state to Setting mode
                .onMessage(SetAllExceptions.class, (SetAllExceptions setAllExceptions) -> {
                    if (setAllExceptions.token.equals(getCmd.token)) {
                        // Tell the replyTo actor the offset that has been received
                        getCmd.replyTo.tell(setAllExceptions);
                        // Stop this actor
                        return Behaviors.stopped();
                    } else {
                        LOGGER.warn("Received setAllExceptions for token '{}', but was expecting AuditErrorMessage for token '{}'", setAllExceptions.token, getCmd.token);
                        return Behaviors.same();
                    }
                })
                .build();
    }

    private Behavior<WorkerCommand> onPostStop(final PostStop ignoredSignal) {
        this.parent.tell(new DeregisterWorker(getContext().getSelf()));
        return this;
    }
}
