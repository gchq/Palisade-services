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
import akka.actor.typed.receptionist.ServiceKey;

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetWorker.WorkerCmd.GetOffset;

class TokenOffsetWorker extends AbstractBehavior<TokenOffsetWorker.WorkerCmd> {
    interface WorkerCmd {
        class GetOffset implements WorkerCmd {
            public final String token;
            public final ActorRef<SetOffset> replyTo;

            GetOffset(final String token, final ActorRef<SetOffset> replyTo) {
                this.token = token;
                this.replyTo = replyTo;
            }
        }

        class SetOffset implements WorkerCmd {
            public final String token;
            public final Long offset;

            SetOffset(final String token, final Long offset) {
                this.token = token;
                this.offset = offset;
            }
        }
    }

    public static Behavior<WorkerCmd> create(final TokenOffsetPersistenceLayer persistenceLayer) {
        return Behaviors.setup(ctx -> new TokenOffsetWorker(ctx, persistenceLayer));
    }

    public static ServiceKey<WorkerCmd> serviceKey(final String token) {
        return ServiceKey.create(WorkerCmd.class, token);
    }

    private final TokenOffsetPersistenceLayer persistenceLayer;

    public TokenOffsetWorker(final ActorContext<WorkerCmd> context, final TokenOffsetPersistenceLayer persistenceLayer) {
        super(context);
        this.persistenceLayer = persistenceLayer;
    }

    @Override
    public Receive<WorkerCmd> createReceive() {
        return this.onGetOffset();
    }

    private Receive<WorkerCmd> onGetOffset() {
        return newReceiveBuilder()
                // Start off in Getting mode
                .onMessage(WorkerCmd.GetOffset.class, getCmd -> {
                    // Get from persistence, if present tell self (if not, will be told in the future)
                    this.persistenceLayer.findOffset(getCmd.token).join()
                            .ifPresent(offset -> this.getContext().getSelf()
                                    .tell(new WorkerCmd.SetOffset(getCmd.token, offset)));
                    return this.onSetOffset(getCmd);
                })
                .build();
    }

    private Receive<WorkerCmd> onSetOffset(final GetOffset getCmd) {
        return newReceiveBuilder()
                // Switch state to Setting mode
                .onMessage(WorkerCmd.SetOffset.class, setOffset -> {
                    // Tell the replyTo actor the offset that has been received
                    getCmd.replyTo.tell(setOffset);
                    // Stop this actor
                    return Behaviors.stopped();
                })
                .build();
    }
}
