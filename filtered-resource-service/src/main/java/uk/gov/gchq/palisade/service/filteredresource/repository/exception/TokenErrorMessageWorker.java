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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageWorker.WorkerCommand;

import java.util.List;

/**
 * A worker to carry-out a client request for a list of AuditErrorMessage (from kafka "error" topic or redis persistence)
 * given a token (from websocket url "ws://filtered-resource-service/resource/$token")
 */
final class TokenErrorMessageWorker extends AbstractBehavior<WorkerCommand> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenErrorMessageWorker.class);


    protected interface WorkerCommand {
        // Marker interface for inputs of the worker
    }

    protected interface WorkerResponse {
        // Marker interface for outputs of the worker
    }

    /**
     * A request to get all AuditErrorMessages for a token.
     * The worker will {@link ActorRef#tell} the replyTo actor the AuditErrorMessages once found.
     */
    protected static class GetAllExceptions implements WorkerCommand {
        protected final String token;
        protected final ActorRef<WorkerResponse> replyTo;

        protected GetAllExceptions(final String token, final ActorRef<WorkerResponse> replyTo) {
            this.token = token;
            this.replyTo = replyTo;
        }
    }

    /**
     * A response for this actor to send to its {@code replyTo} actor.
     * This is received by the worker when appropriate AuditErrorMessages are found.
     * This is both a possible input to the system {@link WorkerCommand} as well as an output {@link WorkerResponse}
     */
    protected static class SetAuditErrorMessages implements WorkerCommand, WorkerResponse {
        protected final String token;
        protected final List<TokenErrorMessageEntity> messageEntities;

        protected SetAuditErrorMessages(final String token, final List<TokenErrorMessageEntity> messageEntities) {
            this.token = token;
            this.messageEntities = messageEntities;
        }
    }

    /**
     * A response for this actor to send to its {@code replyTo} actor.
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
                        .thenApply((List<TokenErrorMessageEntity> listOfEntities) -> {
                            LOGGER.info("token {} and listOfEntities {}", getCmd.token, listOfEntities);
                            getCmd.replyTo.tell(new SetAuditErrorMessages(getCmd.token, listOfEntities));
                            return listOfEntities;
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
