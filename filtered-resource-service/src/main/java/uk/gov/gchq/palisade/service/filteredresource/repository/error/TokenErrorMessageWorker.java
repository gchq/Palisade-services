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
package uk.gov.gchq.palisade.service.filteredresource.repository.error;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageController.TokenErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.WorkerCommand;

import java.util.List;

/**
 * A worker to carry-out a client request for a list of error messages (from redis persistence)
 * given a token (from websocket url "ws://filtered-resource-service/resource/$token")
 */
final class TokenErrorMessageWorker extends AbstractBehavior<WorkerCommand> {


    protected interface WorkerCommand {
        // Marker interface for inputs of the worker
    }

    protected interface WorkerResponse {
        // Marker interface for outputs of the worker
    }

    /**
     * A request to get all error messages for a unique token.
     * The worker will {@link ActorRef#tell} the {@code replyTo} actor the error messages, using
     * the {@link SetAuditErrorMessages} class, once found in persistence.
     */
    protected static class GetAllErrors implements WorkerCommand {
        protected final String token;
        protected final ActorRef<WorkerResponse> replyTo;

        protected GetAllErrors(final String token, final ActorRef<WorkerResponse> replyTo) {
            this.token = token;
            this.replyTo = replyTo;
        }
    }

    /**
     * A response for this actor to send to its {@code replyTo} actor.
     * This is sent by the worker when appropriate error messages are found and output from the system.
     */
    protected static class SetAuditErrorMessages implements WorkerResponse {
        protected final String token;
        protected final List<TokenErrorMessageEntity> messageEntities;

        protected SetAuditErrorMessages(final String token, final List<TokenErrorMessageEntity> messageEntities) {
            this.token = token;
            this.messageEntities = messageEntities;
        }
    }

    /**
     * A response for this actor to send to its guardian actor system.
     * This indicates an exception was thrown by the worker while processing the request.
     *
     * @implNote This is currently only caused by the persistence store throwing an exception.
     */
    protected static class ReportError implements WorkerResponse {
        protected final String token;
        protected final Throwable exception;

        protected ReportError(final String token, final Throwable exception) {
            this.token = token;
            this.exception = exception;
        }
    }

    private final TokenErrorMessagePersistenceLayer persistenceLayer;

    private TokenErrorMessageWorker(final ActorContext<WorkerCommand> context,
                                    final TokenErrorMessagePersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    static Behavior<WorkerCommand> create(final TokenErrorMessagePersistenceLayer persistenceLayer) {
        return Behaviors.setup(ctx -> new TokenErrorMessageWorker(ctx, persistenceLayer));
    }

    /**
     * Default (initial) behaviour for this actor to assume.
     *
     * @return a behaviour that accepts all {@link TokenErrorMessageCommand}s (this actor does not 'become' any other behaviour)
     */
    @Override
    public Receive<WorkerCommand> createReceive() {
        return this.onGetExceptions();
    }

    private Receive<WorkerCommand> onGetExceptions() {
        return newReceiveBuilder()

                // When messaged to get all errors for a given token
                .onMessage(GetAllErrors.class, (GetAllErrors getCmd) -> this.persistenceLayer
                        // Get from persistence
                        .getAllErrorMessages(getCmd.token)
                        // If present emit error message entities to replyTo flow
                        .thenApply((List<TokenErrorMessageEntity> listOfEntities) -> {
                            getCmd.replyTo.tell(new SetAuditErrorMessages(getCmd.token, listOfEntities));
                            return listOfEntities;
                        })
                        // Delete each entity that was returned (so they are not returned again)
                        .thenCompose(this.persistenceLayer::deleteAll)
                        // Stop the actor once done, a new actor will be spawned for any future requests
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
