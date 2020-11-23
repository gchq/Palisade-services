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

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

final class TokenOffsetWorker extends AbstractBehavior<TokenOffsetWorker.WorkerCmd> {
    protected interface WorkerCmd {
        /**
         * A request to get the offset for a token.
         * The worker will {@link ActorRef#tell} the replyTo actor the offset once found.
         */
        class GetOffset implements WorkerCmd {
            protected final String token;
            protected final ActorRef<SetOffset> replyTo;

            protected GetOffset(final String token, final ActorRef<SetOffset> replyTo) {
                this.token = token;
                this.replyTo = replyTo;
            }
        }

        /**
         * A response for this actor to send to its {@code replyTo} actor.
         * This is received by the worker when an appropriate offset if found.
         */
        class SetOffset implements WorkerCmd {
            protected final String token;
            protected final Long offset;

            protected SetOffset(final String token, final Long offset) {
                this.token = token;
                this.offset = offset;
            }
        }
    }

    private final TokenOffsetPersistenceLayer persistenceLayer;

    private TokenOffsetWorker(final ActorContext<WorkerCmd> context, final TokenOffsetPersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    static Behavior<WorkerCmd> create(final TokenOffsetPersistenceLayer persistenceLayer) {
        return Behaviors.setup(ctx -> new TokenOffsetWorker(ctx, persistenceLayer));
    }

    @Override
    public Receive<WorkerCmd> createReceive() {
        return this.onGetOffset();
    }

    private Receive<WorkerCmd> onGetOffset() {
        return newReceiveBuilder()
                // Start off in Getting mode
                .onMessage(WorkerCmd.GetOffset.class, (WorkerCmd.GetOffset getCmd) -> {
                    // Get from persistence, if present tell self (if not, will be told in the future)
                    this.persistenceLayer.findOffset(getCmd.token).join()
                            .ifPresent(offset -> this.getContext().getSelf()
                                    .tell(new WorkerCmd.SetOffset(getCmd.token, offset)));
                    return this.onSetOffset(getCmd);
                })
                .build();
    }

    private Receive<WorkerCmd> onSetOffset(final WorkerCmd.GetOffset getCmd) {
        return newReceiveBuilder()
                // Switch state to Setting mode
                .onMessage(WorkerCmd.SetOffset.class, (WorkerCmd.SetOffset setOffset) -> {
                    // Tell the replyTo actor the offset that has been received
                    getCmd.replyTo.tell(setOffset);
                    // Stop this actor
                    return Behaviors.stopped();
                })
                .build();
    }
}
