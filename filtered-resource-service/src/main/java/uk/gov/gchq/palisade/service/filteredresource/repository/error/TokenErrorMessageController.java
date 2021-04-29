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

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.typed.javadsl.ActorFlow;

import uk.gov.gchq.palisade.service.filteredresource.model.TokenErrorMessagePersistenceResponse;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageController.TokenErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.GetAllErrors;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.ReportError;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.SetAuditErrorMessages;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.WorkerCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageWorker.WorkerResponse;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.UUID;

import static uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessageController.TokenErrorMessageCommand.SpawnWorker;

/**
 * Manages getting from the persistence layer such that requests will wait until AuditErrorMessages arrive.
 * This aims to remove any long-polling of the persistence store (redis) while ensuring AuditErrorMessages from
 * kafka can be set before -or- after they are retrieved by the client's websocket handler.
 */
public final class TokenErrorMessageController extends AbstractBehavior<TokenErrorMessageCommand> {

    /**
     * Command to the ActorSystem
     */
    public interface TokenErrorMessageCommand extends WorkerCommand {
        /**
         * A request to spawn a new worker, which will acquire a list of all current AuditErrorMessages in
         * persistence for a token and reply back with them. These are deleted from persistence upon being
         * returned.
         */
        class SpawnWorker implements TokenErrorMessageCommand {
            protected final GetAllErrors getAllErrors;

            /**
             * A request to spawn a new worker, which will acquire a List of AuditErrorMessages for a token and reply back
             *
             * @param token   the unique token for the request
             * @param replyTo the {@link ActorRef} to reply to once the AuditErrorMessages are found
             */
            public SpawnWorker(final String token, final ActorRef<WorkerResponse> replyTo) {
                this.getAllErrors = new GetAllErrors(token, replyTo);
            }
        }
    }

    // Timeout on the ActorFlow::ask
    // The unit can't be longer than DAYS (anything more is an estimated duration)
    // Despite this, we can just put the max value akka will accept, which seems to be about 248 days
    private static final Duration TIMEOUT = Duration.of(Integer.MAX_VALUE / 100, ChronoUnit.SECONDS);

    private final TokenErrorMessagePersistenceLayer persistenceLayer;

    private TokenErrorMessageController(final ActorContext<TokenErrorMessageCommand> context, final TokenErrorMessagePersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Create a new {@link TokenErrorMessageController} and start it running.
     *
     * @param persistenceLayer a reference to the persistence, used to retrieve AuditErrorMessages for the unique token
     * @return an {@link ActorRef} to a running {@link ActorSystem} that accepts {@link TokenErrorMessageCommand}
     */
    public static ActorRef<TokenErrorMessageCommand> create(final TokenErrorMessagePersistenceLayer persistenceLayer) {
        Behavior<TokenErrorMessageCommand> behavior = Behaviors.setup(ctx -> new TokenErrorMessageController(ctx, persistenceLayer));
        return ActorSystem.create(behavior, TokenErrorMessageController.class.getSimpleName());
    }

    /**
     * Create a {@link Flow} that will ask the given actor with each of the elements, spawning a worker to retrieve AuditErrorMessages for a token.
     *
     * @param actorSystem an {@link ActorRef} to the {@link ActorSystem} created by the {@link TokenErrorMessageController#create(TokenErrorMessagePersistenceLayer)} method
     * @return a {@link Flow} from tokens to {@link Pair}s of tokens and their AuditErrorMessage Lists
     */
    public static Flow<String, TokenErrorMessagePersistenceResponse, NotUsed> asGetterFlow(final ActorRef<TokenErrorMessageCommand> actorSystem) {
        return ActorFlow.ask(actorSystem, TIMEOUT, SpawnWorker::new)
                .map((WorkerResponse workerResponse) -> {

                    if (workerResponse instanceof SetAuditErrorMessages) {
                        // If there are TokenErrorMessageEntities retrieved from persistence for the token
                        SetAuditErrorMessages setAuditErrorMessages = (SetAuditErrorMessages) workerResponse;
                        return TokenErrorMessagePersistenceResponse.Builder.create()
                                .withToken(setAuditErrorMessages.token)
                                .withMessageEntities(setAuditErrorMessages.messageEntities);

                    } else if (workerResponse instanceof ReportError) {
                        // If there was an issue retrieving the message from persistence then report an error
                        ReportError reportError = (ReportError) workerResponse;
                        return TokenErrorMessagePersistenceResponse.Builder.create()
                                .withToken(reportError.token)
                                .withException(reportError.exception);

                    } else {
                        // This can only occur if the WorkerResponse subclasses are extended and not accounted for here
                        // Entirely developer error, and provides a return from this if statement
                        throw new NoSuchElementException(String.format(
                                "Could not determine input class type %s as a subclass of %s",
                                workerResponse.getClass(), WorkerResponse.class));
                    }
                });
    }

    /**
     * Default (initial) behaviour for this actor to assume.
     *
     * @return a behaviour that accepts all {@link TokenErrorMessageCommand}s (this actor does not 'become' any other behaviour)
     */
    @Override
    public Receive<TokenErrorMessageCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SpawnWorker.class, this::onSpawnWorker)
                .build();
    }

    /**
     * The {@link SpawnWorker} command will spawn a {@link TokenErrorMessageWorker} and pass it the command's token.
     * After the worker has been spawned, it looks for all AuditErrorMessages that link to a token.
     * The actor is given a UUID to remain unique, as there may be multiple requests for errors for a single token.
     * The command's {@code replyTo} will later {@link ActorRef#tell} the AuditErrorMessages for that token.
     *
     * @param spawnWorker the {@link SpawnWorker} command to handle
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenErrorMessageCommand> onSpawnWorker(final SpawnWorker spawnWorker) {
        // Spawn a new worker, passing it the persistenceLayer to query
        Behavior<WorkerCommand> workerBehavior = TokenErrorMessageWorker.create(this.persistenceLayer);
        ActorRef<WorkerCommand> workerRef = this.getContext()
                .spawn(workerBehavior, spawnWorker.getAllErrors.token + "_" + UUID.randomUUID());
        // Tell our worker to 'start-up' (check persistence)
        workerRef.tell(new GetAllErrors(spawnWorker.getAllErrors.token, spawnWorker.getAllErrors.replyTo));
        return this;
    }
}
