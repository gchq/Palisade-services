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
import akka.actor.typed.internal.receptionist.ReceptionistMessages.Find;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.Receptionist.Listing;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorFlow;

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetWorker.WorkerCmd;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class TokenOffsetActorSystem extends AbstractBehavior<TokenOffsetActorSystem.TokenOffsetCmd> {
    public interface TokenOffsetCmd {
        class SpawnWorker extends WorkerCmd.GetOffset implements TokenOffsetCmd {
            public SpawnWorker(final String token, final ActorRef<WorkerCmd.SetOffset> replyTo) {
                super(token, replyTo);
            }
        }

        class AckSetOffset extends Pair<WorkerCmd.SetOffset, ActorRef<WorkerCmd.SetOffset>> implements TokenOffsetCmd {
            public AckSetOffset(final Pair<String, Long> copy, final ActorRef<WorkerCmd.SetOffset> ackTo) {
                super(new WorkerCmd.SetOffset(copy), ackTo);
            }
        }

        class ListingResponse implements TokenOffsetCmd {
            final Listing listing;

            public ListingResponse(final Listing listing) {
                this.listing = listing;
            }
        }
    }

    private static final Duration TIMEOUT = Duration.ofDays(1);

    public static ActorSystem<TokenOffsetCmd> create(final TokenOffsetPersistenceLayer persistenceLayer) {
        Behavior<TokenOffsetCmd> behavior = Behaviors.setup(ctx -> new TokenOffsetActorSystem(ctx, persistenceLayer));
        return ActorSystem.create(behavior, TokenOffsetActorSystem.class.getSimpleName());
    }

    public static Sink<Pair<String, Long>, NotUsed> asSetterSink(final ActorRef<TokenOffsetCmd> tokenOffsetSystem) {
        return ActorFlow.ask(tokenOffsetSystem, TIMEOUT, TokenOffsetCmd.AckSetOffset::new)
                // Easier to do ActorFlow -> Sink.ignore than ActorSink
                .to(Sink.ignore());
    }

    public static Flow<String, Pair<String, Long>, NotUsed> asGetterFlow(final ActorRef<TokenOffsetCmd> tokenOffsetSystem) {
        return ActorFlow.ask(tokenOffsetSystem, TIMEOUT, TokenOffsetCmd.SpawnWorker::new)
                // Downcast SetOffset to Pair<String, Long>
                .map(x -> x);
    }

    private final TokenOffsetPersistenceLayer persistenceLayer;
    private final ActorRef<Listing> listingResponseAdapter;
    private final HashMap<ServiceKey<WorkerCmd>, WorkerCmd.SetOffset> inFlightResponses = new HashMap<>();

    public TokenOffsetActorSystem(final ActorContext<TokenOffsetCmd> context, final TokenOffsetPersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
        this.listingResponseAdapter = context.messageAdapter(Receptionist.Listing.class, TokenOffsetCmd.ListingResponse::new);
    }

    @Override
    public Receive<TokenOffsetCmd> createReceive() {
        return newReceiveBuilder()
                .onMessage(TokenOffsetCmd.SpawnWorker.class, this::onSpawnWorker)
                .onMessage(TokenOffsetCmd.AckSetOffset.class, this::onAckSetOffset)
                .onMessage(TokenOffsetCmd.ListingResponse.class, this::onListingResponse)
                .build();
    }

    private Behavior<TokenOffsetCmd> onSpawnWorker(final TokenOffsetCmd.SpawnWorker spawnWorker) {
        ServiceKey<WorkerCmd> serviceKey = TokenOffsetWorker.serviceKey(spawnWorker.first());
        // Spawn a new worker
        ActorRef<WorkerCmd> workerRef = this.getContext()
                .spawn(TokenOffsetWorker.create(this.persistenceLayer), serviceKey.id());
        // Tell our worker to 'start-up' (check persistence and maybe wait to be told a SetOffset)
        workerRef.tell(new WorkerCmd.GetOffset(spawnWorker.first(), spawnWorker.second()));
        // Tell the receptionist that our worker exists
        this.getContext().getSystem().receptionist()
                .tell(Receptionist.register(serviceKey, workerRef));
        return this;
    }

    private Behavior<TokenOffsetCmd> onAckSetOffset(final TokenOffsetCmd.AckSetOffset ackSetOffset) {
        Find<WorkerCmd> findWorker = new Find<>(TokenOffsetWorker.serviceKey(ackSetOffset.first().first()), this.listingResponseAdapter);
        // Register the find with the receptionist -> this::onListingResponse
        this.getContext().getSystem().receptionist()
                .tell(findWorker);
        // Store additional context for this worker key
        this.inFlightResponses.put(findWorker.key(), ackSetOffset.first());
        // Acknowledge this message
        ackSetOffset.second().tell(ackSetOffset.first());
        return this;
    }

    private Behavior<TokenOffsetCmd> onListingResponse(final TokenOffsetCmd.ListingResponse listingResponse) {
        ServiceKey<WorkerCmd> serviceKey = (ServiceKey<WorkerCmd>) listingResponse.listing.getKey();
        // Get the context stored for this worker key
        WorkerCmd.SetOffset setOffset = this.inFlightResponses.get(serviceKey);
        // Tell the setOffset (stored in the map) to all actors
        listingResponse.listing.getAllServiceInstances(serviceKey)
                .forEach((ActorRef<WorkerCmd> actorRef) -> actorRef.tell(setOffset));
        // Remove context stored for this worker key
        this.inFlightResponses.remove(serviceKey);
        return this;
    }


}
