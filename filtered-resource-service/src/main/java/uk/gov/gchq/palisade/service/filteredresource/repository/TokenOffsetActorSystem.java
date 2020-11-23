/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.service.filteredresource.repository;

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

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetWorker.WorkerCmd;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;

/**
 * Manages getting from the persistence layer such that requests are 'blocked' until an offset arrives.
 * This aims to remove any long-polling of the persistence store (redis) while ensuring offsets from
 * kafka can be set before -or- after they are got by the client's websocket handler.
 */
public final class TokenOffsetActorSystem extends AbstractBehavior<TokenOffsetActorSystem.TokenOffsetCmd> {
    /**
     * Command to the token-offset-actor-system.
     * This can either be a request to spawn a new worker, which will acquire the offset for a token and reply back,
     * or a request to tell running workers about a newly-discovered offset for a given token.
     */
    public interface TokenOffsetCmd extends WorkerCmd {
        /**
         * A request to spawn a new worker, which will acquire the offset for a token and reply back
         */
        class SpawnWorker implements TokenOffsetCmd {
            protected final WorkerCmd.GetOffset getOffset;

            /**
             * A request to spawn a new worker, which will acquire the offset for a token and reply back
             *
             * @param token   the client's token for which an offset was requested
             * @param replyTo the {@link ActorRef} to reply to once the offset is found
             */
            public SpawnWorker(final String token, final ActorRef<WorkerCmd.SetOffset> replyTo) {
                this.getOffset = new GetOffset(token, replyTo);
            }
        }

        /**
         * A request to tell running workers about a newly-discovered offset for a given token.
         */
        class AckTellWorker implements TokenOffsetCmd {
            protected final WorkerCmd.SetOffset setOffset;
            protected final ActorRef<WorkerCmd.SetOffset> ackRef;

            /**
             * A request to tell running workers about a newly-discovered offset for a given token.
             *
             * @param setOffsetPair a pair of a token and its offset
             * @param ackRef        an {@link ActorRef} to reply to to acknowledge the message has been processed
             */
            public AckTellWorker(final Pair<String, Long> setOffsetPair, final ActorRef<WorkerCmd.SetOffset> ackRef) {
                this.setOffset = new SetOffset(setOffsetPair.first(), setOffsetPair.second());
                this.ackRef = ackRef;
            }
        }
    }

    // Timeout on the ActorFlow::ask
    // The unit can't be longer than DAYS (anything more is an estimated duration)
    // Despite this, we can just put the max value akka will accept, which seems to be about 248 days
    private static final Duration TIMEOUT = Duration.of(Integer.MAX_VALUE / 100, ChronoUnit.SECONDS);

    private final TokenOffsetPersistenceLayer persistenceLayer;
    // Map from Tokens to TokenOffsetWorker actors
    private final HashMap<String, ActorRef<WorkerCmd>> inFlightWorkers = new HashMap<>();

    private TokenOffsetActorSystem(final ActorContext<TokenOffsetCmd> context, final TokenOffsetPersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Create a new {@link TokenOffsetActorSystem} and start it running.
     *
     * @param persistenceLayer a persistence layer to try to read from (waiting for messages to the
     *                         {@link TokenOffsetActorSystem#asSetterSink(ActorRef)} if the token is
     *                         not found)
     * @return an {@link ActorRef} to a running {@link ActorSystem} that accepts {@link TokenOffsetCmd}s
     */
    public static ActorRef<TokenOffsetCmd> create(final TokenOffsetPersistenceLayer persistenceLayer) {
        Behavior<TokenOffsetCmd> behavior = Behaviors.setup(ctx -> new TokenOffsetActorSystem(ctx, persistenceLayer));
        return ActorSystem.create(behavior, TokenOffsetActorSystem.class.getSimpleName());
    }

    /**
     * Create a {@link Sink} for updates to the persistence store
     *
     * @param tokenOffsetActor an {@link ActorRef} to the {@link ActorSystem} created by the
     *                         {@link TokenOffsetActorSystem#create(TokenOffsetPersistenceLayer)} method
     * @return a {@link Sink} accepting pairs of tokens and their offset, which will be told to any appropriate {@link TokenOffsetWorker}s
     * @implNote It is required that the persistence store will be written to before the sink is used:
     * <pre>{@code
     *    ActorRef<TokenOffsetCmd> tokenOffsetActor = TokenOffsetActorSystem.create(persistenceLayer);
     *    // ...
     *    source
     *        .mapAsync((token, offset) -> persistenceLayer.putOffset(token, offset)
     *                .thenApply(ignored -> (token, offset)))
     *        .to(TokenOffsetActorSystem.asSetterSink(tokenOffsetActor))
     *        .run(actorSystem);
     * }</pre>
     */
    public static Sink<Pair<String, Long>, NotUsed> asSetterSink(final ActorRef<TokenOffsetCmd> tokenOffsetActor) {
        return ActorFlow.ask(tokenOffsetActor, TIMEOUT, TokenOffsetCmd.AckTellWorker::new)
                // Easier to do ActorFlow().to(Sink.ignore) than ActorSink()
                .to(Sink.ignore());
    }

    /**
     * Create a {@link Flow} that will ask the given actor with each of the elements, spawning a worker to carry out the
     * request for an offset. The Flow of tokens will be joined with their matching kafka commit offset.
     *
     * @param tokenOffsetActor an {@link ActorRef} to the {@link ActorSystem} created by the
     *                         {@link TokenOffsetActorSystem#create(TokenOffsetPersistenceLayer)} method
     * @return a {@link Flow} from tokens to {@link Pair}s of tokens and their offsets
     * @implNote The {@link Flow} remains strictly ordered, so a long-blocking first element will delay all subsequent
     * elements.
     */
    public static Flow<String, Pair<String, Long>, NotUsed> asGetterFlow(final ActorRef<TokenOffsetCmd> tokenOffsetActor) {
        return ActorFlow.ask(tokenOffsetActor, TIMEOUT, TokenOffsetCmd.SpawnWorker::new)
                // Downcast SetOffset to Pair<String, Long>
                .map(setOffset -> Pair.create(setOffset.token, setOffset.offset));
    }

    /**
     * Default (initial) behaviour for this actor to assume.
     *
     * @return a behaviour that accepts all {@link TokenOffsetCmd}s (this actor does not 'become' any other behaviour)
     */
    @Override
    public Receive<TokenOffsetCmd> createReceive() {
        return newReceiveBuilder()
                .onMessage(TokenOffsetCmd.SpawnWorker.class, this::onSpawnWorker)
                .onMessage(TokenOffsetCmd.AckTellWorker.class, this::onAckSetOffset)
                .build();
    }

    /**
     * The {@link TokenOffsetCmd.SpawnWorker} command will spawn a {@link TokenOffsetWorker} and pass it the command's token
     * ({@link Pair#first()}).
     * After the worker has been spawned, it is added to an internal map of Tokens to TokenOffsetWorkers for tokens currently
     * in flight. This is later got from the map by the {@link TokenOffsetActorSystem#onAckSetOffset(TokenOffsetCmd.AckTellWorker)}
     * method. The command's replyTo ({@link Pair#second()}) will later {@link ActorRef#tell} the offset for that token. This
     * offset may come from persistence, or it may have been told to the actor through an {@link TokenOffsetCmd.AckTellWorker}
     * command.
     *
     * @param spawnWorker the {@link TokenOffsetCmd.SpawnWorker} command to handle
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenOffsetCmd> onSpawnWorker(final TokenOffsetCmd.SpawnWorker spawnWorker) {
        // Spawn a new worker
        ActorRef<WorkerCmd> workerRef = this.getContext()
                .spawn(TokenOffsetWorker.create(this.persistenceLayer), spawnWorker.getOffset.token);
        // Tell our worker to 'start-up' (check persistence and maybe wait to be told a SetOffset)
        workerRef.tell(new WorkerCmd.GetOffset(spawnWorker.getOffset.token, spawnWorker.getOffset.replyTo));
        // Register that our worker exists
        this.inFlightWorkers.put(spawnWorker.getOffset.token, workerRef);
        return this;
    }

    /**
     * The {@link TokenOffsetCmd.AckTellWorker} command will tell any {@link TokenOffsetWorker}s for the command's token that an
     * offset has been set ({@link Pair#first()}). This is done through storing the worker's {@link ActorRef} in an internal map,
     * then looking up any {@link ActorRef}s matching the token. The former step (storing the worker actor) is done as part of
     * the  {@link TokenOffsetActorSystem#onSpawnWorker(TokenOffsetCmd.SpawnWorker)} method when the worker is created.
     *
     * @param ackTellWorker the {@link TokenOffsetCmd.AckTellWorker} command to handle
     * @return an unchanged {@link Behavior} (this actor does not 'become' any other behaviour)
     */
    private Behavior<TokenOffsetCmd> onAckSetOffset(final TokenOffsetCmd.AckTellWorker ackTellWorker) {
        Optional.ofNullable(this.inFlightWorkers.get(ackTellWorker.setOffset.token))
                .ifPresent((ActorRef<WorkerCmd> workerRef) -> {
                    workerRef.tell(ackTellWorker.setOffset);
                    this.inFlightWorkers.remove(ackTellWorker.setOffset.token, workerRef);
                });
        // Acknowledge this message
        ackTellWorker.ackRef.tell(ackTellWorker.setOffset);
        return this;
    }

}
