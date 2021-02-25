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

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageController.TokenAuditErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.GetAllExceptions;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.ReportError;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.SetAllExceptions;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.WorkerCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessageWorker.WorkerResponse;

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
public final class TokenAuditErrorMessageController extends AbstractBehavior<TokenAuditErrorMessageCommand> {

    public interface TokenAuditErrorMessageCommand extends WorkerCommand {
        class SpawnWorker implements TokenAuditErrorMessageCommand {
            protected final GetAllExceptions getAllExceptions;

            public SpawnWorker(final String token, final ActorRef<WorkerResponse> replyTo) {
                this.getAllExceptions = new GetAllExceptions(token, replyTo);
            }
        }

        class AckTellWorker implements TokenAuditErrorMessageCommand {
            protected final SetAllExceptions setAllExceptions;
            protected final ActorRef<SetAllExceptions> ackRef;

            public AckTellWorker(final Pair<String, AuditErrorMessage> setAuditErrorMessagePair, final ActorRef<SetAllExceptions> ackRef) {
                this.setAllExceptions = new SetAllExceptions(setAuditErrorMessagePair.first(), setAuditErrorMessagePair.second());
                this.ackRef = ackRef;
            }
        }
    }

    protected static class DeregisterWorker implements TokenAuditErrorMessageCommand {
        protected final ActorRef<WorkerCommand> workerRef;

        public DeregisterWorker(final ActorRef<WorkerCommand> workerRef) {
            this.workerRef = workerRef;
        }
    }

    // Timeout on the ActorFlow::ask
    // The unit can't be longer than DAYS (anything more is an estimated duration)
    // Despite this, we can just put the max value akka will accept, which seems to be about 248 days
    private static final Duration TIMEOUT = Duration.of(Integer.MAX_VALUE / 100, ChronoUnit.SECONDS);

    private final TokenAuditErrorMessagePersistenceLayer persistenceLayer;
    // Map from Tokens to TokenOffsetWorker actors
    private final ConcurrentHashMap<String, ActorRef<WorkerCommand>> workers = new ConcurrentHashMap<>();

    private TokenAuditErrorMessageController(final ActorContext<TokenAuditErrorMessageCommand> context, final TokenAuditErrorMessagePersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    public static ActorRef<TokenAuditErrorMessageCommand> create(final TokenAuditErrorMessagePersistenceLayer persistenceLayer) {
        Behavior<TokenAuditErrorMessageCommand> behavior = Behaviors.setup(ctx -> new TokenAuditErrorMessageController(ctx, persistenceLayer));
        return ActorSystem.create(behavior, TokenAuditErrorMessageController.class.getSimpleName());
    }

    public static Sink<Pair<String, AuditErrorMessage>, NotUsed> asSetterSink(final ActorRef<TokenAuditErrorMessageCommand> tokenAuditErrorMessageActor) {
        return ActorFlow.ask(tokenAuditErrorMessageActor, TIMEOUT, TokenAuditErrorMessageCommand.AckTellWorker::new)
                .to(Sink.ignore());
    }

    public static Flow<String, TokenAuditErrorMessageEntity, NotUsed> asGetterFlow(final ActorRef<TokenAuditErrorMessageCommand> tokenOffsetActor) {
        return ActorFlow.ask(tokenOffsetActor, TIMEOUT, TokenAuditErrorMessageCommand.SpawnWorker::new)
                .map((WorkerResponse workerResponse) -> {
                    if (workerResponse instanceof SetAllExceptions) {
                        SetAllExceptions exceptions = (SetAllExceptions) workerResponse;
                        return new TokenAuditErrorMessageEntity(exceptions.token, exceptions.auditErrorMessage.get(1));
                    } else if (workerResponse instanceof ReportError) {
                        ReportError reportError = (ReportError) workerResponse;
                        return new TokenAuditErrorMessageEntity(reportError.token, AuditErrorMessage.Builder.create()
                                .withUserId(null).withResourceId(null).withContext(null).withAttributes(null).withError(reportError.exception));
                    } else {
                        // This can only occur if the WorkerResponse subclasses are extended and not accounted for here
                        // Entirely developer error, and provides a return from this if statement
                        throw new NoSuchElementException(String.format(
                                "Could not determine input class type %s as a subclass of %s",
                                workerResponse.getClass(), WorkerResponse.class));
                    }
                });
    }

    @Override
    public Receive<TokenAuditErrorMessageCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TokenAuditErrorMessageCommand.SpawnWorker.class, this::onSpawnWorker)
                .onMessage(TokenAuditErrorMessageCommand.AckTellWorker.class, this::onAckSetOffset)
                .onMessage(DeregisterWorker.class, this::onDeregisterWorker)
                .build();
    }

    private Behavior<TokenAuditErrorMessageCommand> onSpawnWorker(final TokenAuditErrorMessageCommand.SpawnWorker spawnWorker) {
        // Spawn a new worker, passing this (its parent) as a 'callback' for Deregister commands
        Behavior<WorkerCommand> workerBehavior = TokenAuditErrorMessageWorker.create(this.persistenceLayer, this.getContext().getSelf().narrow());
        ActorRef<WorkerCommand> workerRef = this.getContext()
                .spawn(workerBehavior, spawnWorker.getAllExceptions.token);
        // Tell our worker to 'start-up' (check persistence and maybe wait to be told a SetOffset)
        workerRef.tell(new GetAllExceptions(spawnWorker.getAllExceptions.token, spawnWorker.getAllExceptions.replyTo));
        // Register that our worker exists
        this.workers.put(spawnWorker.getAllExceptions.token, workerRef);
        return this;
    }

    private Behavior<TokenAuditErrorMessageCommand> onAckSetOffset(final TokenAuditErrorMessageCommand.AckTellWorker ackTellWorker) {
        Optional.ofNullable(this.workers.get(ackTellWorker.setAllExceptions.token))
                .ifPresent((ActorRef<WorkerCommand> workerRef) -> workerRef.tell(ackTellWorker.setAllExceptions));
        // Acknowledge this message
        ackTellWorker.ackRef.tell(ackTellWorker.setAllExceptions);
        return this;
    }

    private Behavior<TokenAuditErrorMessageCommand> onDeregisterWorker(final DeregisterWorker deregisterWorker) {
        // Deregister all workers matching the actorRef supplied in the message
        this.workers.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(deregisterWorker.workerRef) == 0)
                .forEach(entry -> this.workers.remove(entry.getKey()));
        return this;
    }

}
