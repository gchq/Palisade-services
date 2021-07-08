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
import akka.stream.javadsl.Sink;
import akka.stream.typed.javadsl.ActorFlow;

import uk.gov.gchq.palisade.service.filteredresource.model.TokenOffsetPersistenceResponse;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.GetOffset;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.ReportError;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.SetOffset;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.WorkerCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.WorkerResponse;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages getting from the persistence layer such that requests will wait until an offset arrives.
 * This aims to remove any long-polling of the persistence store (redis) while ensuring offsets from
 * kafka can be set before -or- after they are got by the client's websocket handler.
 */
public final class TokenOffsetController extends AbstractBehavior<TokenOffsetCommand> {
    /**
     * Command to the ActorSystem. This can either be a request to spawn a new worker, which will
     * acquire the offset for a token and reply back, or a request to tell running workers about
     * a newly-discovered offset for a given token.
     */
    public interface TokenOffsetCommand extends WorkerCommand {
        /**
         * A request to spawn a new worker, which will acquire the offset for a token and reply back
         */
        class SpawnWorker implements TokenOffsetCommand {
            protected final GetOffset getOffset;

            /**
             * A request to spawn a new worker, which will acquire the offset for a token and reply back
             *
             * @param token   the client's token for which an offset was requested
             * @param replyTo the {@link ActorRef} to reply to once the offset is found
             */
            public SpawnWorker(final String token, final ActorRef<WorkerResponse> replyTo) {
                this.getOffset = new GetOffset(token, replyTo);
            }
        }

        /**
         * A request to tell running workers about a newly-discovered offset for a given token.
         */
        class AckTellWorker implements TokenOffsetCommand {
            protected final SetOffset setOffset;
            protected final ActorRef<SetOffset> ackRef;

            /**
             * A request to tell running workers about a newly-discovered offset for a given token.
             *
             * @param setOffsetPair a pair of a token and its offset
             * @param ackRef        an {@link ActorRef} to reply to to acknowledge the message has been processed
             */
            public AckTellWorker(final Pair<String, Long> setOffsetPair, final ActorRef<SetOffset> ackRef) {
                this.setOffset = new SetOffset(setOffsetPair.first(), setOffsetPair.second());
                this.ackRef = ackRef;
            }
        }
    }

    protected static class DeregisterWorker implements TokenOffsetCommand {
        protected final ActorRef<WorkerCommand> workerRef;

        public DeregisterWorker(final ActorRef<WorkerCommand> workerRef) {
            this.workerRef = workerRef;
        }
    }

    // Timeout on the ActorFlow::ask
    // The unit can't be longer than DAYS (anything more is an estimated duration)
    // Despite this, we can just put the max value akka will accept, which seems to be about 248 days
    private static final Duration TIMEOUT = Duration.of(Integer.MAX_VALUE / 100, ChronoUnit.SECONDS);

    private final TokenOffsetPersistenceLayer persistenceLayer;
    // Map from Tokens to TokenOffsetWorker actors
    private final ConcurrentHashMap<String, ActorRef<WorkerCommand>> workers = new ConcurrentHashMap<>();

    private TokenOffsetController(final ActorContext<TokenOffsetCommand> context, final TokenOffsetPersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Create a new {@link TokenOffsetController} and start it running.
     *
     * @param persistenceLayer a persistence layer to try to read from (waiting for messages to the
     *                         {@link TokenOffsetController#asSetterSink(ActorRef)} if the token is
     *                         not found)
     * @return an {@link ActorRef} to a running {@link ActorSystem} that accepts {@link TokenOffsetCommand}s
     */
    public static ActorSystem<TokenOffsetCommand> create(final TokenOffsetPersistenceLayer persistenceLayer) {
        Behavior<TokenOffsetCommand> behavior = Behaviors.setup(ctx -> new TokenOffsetController(ctx, persistenceLayer));
        return ActorSystem.create(behavior, TokenOffsetController.class.getSimpleName());
    }

    /**
     * Create a {@link Sink} for updates to the persistence store
     *
     * @param tokenOffsetActor an {@link ActorRef} to the {@link ActorSystem} created by the
     *                         {@link TokenOffsetController#create(TokenOffsetPersistenceLayer)} method
     * @return a {@link Sink} accepting pairs of tokens and their offset, which will be told to any appropriate {@link TokenOffsetWorker}s
     * @implNote It is required that the persistence store will be written to before the sink is used:
     * <pre>{@code
     *    ActorRef<TokenOffsetCommand> tokenOffsetActor = TokenOffsetController.create(persistenceLayer);
     *    // ...
     *    source
     *        .mapAsync((token, offset) -> persistenceLayer.putOffset(token, offset)
     *                .thenApply(ignored -> (token, offset)))
     *        .to(TokenOffsetController.asSetterSink(tokenOffsetActor))
     *        .run(actorSystem);
     * }</pre>
     */
    public static Sink<Pair<String, Long>, NotUsed> asSetterSink(final ActorRef<TokenOffsetCommand> tokenOffsetActor) {
        return ActorFlow.ask(tokenOffsetActor, TIMEOUT, TokenOffsetCommand.AckTellWorker::new)
                .to(Sink.ignore());
    }

    /**
     * Create a {@link Flow} that will ask the given actor with each of the elements, spawning a worker to carry out the
     * request for an offset. The Flow of tokens will be joined with their matching kafka commit offset.
     *
     * @param tokenOffsetActor an {@link ActorRef} to the {@link ActorSystem} created by the
     *                         {@link TokenOffsetController#create(TokenOffsetPersistenceLayer)} method
     * @return a {@link Flow} from tokens to {@link Pair}s of tokens and their offsets
     * @implNote The {@link Flow} remains strictly ordered, so a long-blocking first element will delay all subsequent
     * elements.
     */
    @SuppressWarnings("java:S1905") // Cast to SetOffset/ReportError is not unnecessary
    public static Flow<String, TokenOffsetPersistenceResponse, NotUsed> asGetterFlow(final ActorRef<TokenOffsetCommand> tokenOffsetActor) {
        return ActorFlow.ask(tokenOffsetActor, TIMEOUT, TokenOffsetCommand.SpawnWorker::new)
                .map((WorkerResponse workerResponse) -> {
                    if (workerResponse instanceof SetOffset) {
                        SetOffset setOffset = (SetOffset) workerResponse;
                        return TokenOffsetPersistenceResponse.Builder.create()
                                .withToken(setOffset.token)
                                .withOffset(setOffset.offset);
                    } else if (workerResponse instanceof ReportError) {
                        ReportError reportError = (ReportError) workerResponse;
                        return TokenOffsetPersistenceResponse.Builder.create()
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
     * @return a behaviour that accepts all {@link TokenOffsetCommand}s (this actor does not 'become' any other behaviour)
     */
    @Override
    public Receive<TokenOffsetCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TokenOffsetCommand.SpawnWorker.class, this::onSpawnWorker)
                .onMessage(TokenOffsetCommand.AckTellWorker.class, this::onAckSetOffset)
                .onMessage(DeregisterWorker.class, this::onDeregisterWorker)
                .build();
    }

    /**
     * The {@link TokenOffsetCommand.SpawnWorker} command will spawn a {@link TokenOffsetWorker} and pass it the command's token
     * ({@link Pair#first()}).
     * After the worker has been spawned, it is added to an internal map of Tokens to TokenOffsetWorkers for tokens currently in flight.
     * Workers added to the map will deregister themselves before stopping.
     * Workers added to the map are used when got from the map by the {@link TokenOffsetController#onAckSetOffset(TokenOffsetCommand.AckTellWorker)}
     * method. The command's {@code replyTo} will later {@link ActorRef#tell} the offset for that token.
     * This offset may come from persistence, or it may have been told to the actor through an {@link TokenOffsetCommand.AckTellWorker}
     * command.
     *
     * @param spawnWorker the {@link TokenOffsetCommand.SpawnWorker} command to handle
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenOffsetCommand> onSpawnWorker(final TokenOffsetCommand.SpawnWorker spawnWorker) {
        // Spawn a new worker, passing this (its parent) as a 'callback' for Deregister commands
        Behavior<WorkerCommand> workerBehavior = TokenOffsetWorker.create(this.persistenceLayer, this.getContext().getSelf().narrow());
        ActorRef<WorkerCommand> workerRef = this.getContext()
                .spawn(workerBehavior, spawnWorker.getOffset.token);
        // Tell our worker to 'start-up' (check persistence and maybe wait to be told a SetOffset)
        workerRef.tell(new GetOffset(spawnWorker.getOffset.token, spawnWorker.getOffset.replyTo));
        // Register that our worker exists
        this.workers.put(spawnWorker.getOffset.token, workerRef);
        return this;
    }

    /**
     * The {@link TokenOffsetCommand.AckTellWorker} command will tell any {@link TokenOffsetWorker}s for the command's token that an
     * offset has been set ({@link Pair#first()}). This is done through storing the worker's {@link ActorRef} in an internal map,
     * then looking up any {@link ActorRef}s matching the token. The former step (storing the worker actor) is done as part of
     * the  {@link TokenOffsetController#onSpawnWorker(TokenOffsetCommand.SpawnWorker)} method when the worker is created.
     *
     * @param ackTellWorker the {@link TokenOffsetCommand.AckTellWorker} command to handle
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenOffsetCommand> onAckSetOffset(final TokenOffsetCommand.AckTellWorker ackTellWorker) {
        Optional.ofNullable(this.workers.get(ackTellWorker.setOffset.token))
                .ifPresent((ActorRef<WorkerCommand> workerRef) -> workerRef.tell(ackTellWorker.setOffset));
        // Acknowledge this message
        ackTellWorker.ackRef.tell(ackTellWorker.setOffset);
        return this;
    }

    /**
     * When workers enter the {@link Behaviors#stopped()} state, they will intercept the {@link akka.actor.typed.PostStop}
     * signal and deregister themselves with the parent actorSystem.
     * This will remove the worker from the map.
     *
     * @param deregisterWorker the command to deregister a child worker
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenOffsetCommand> onDeregisterWorker(final DeregisterWorker deregisterWorker) {
        // Deregister all workers matching the actorRef supplied in the message
        this.workers.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(deregisterWorker.workerRef) == 0)
                .forEach(entry -> this.workers.remove(entry.getKey()));
        return this;
    }

}
