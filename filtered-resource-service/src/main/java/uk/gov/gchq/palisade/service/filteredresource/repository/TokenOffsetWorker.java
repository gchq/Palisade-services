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
import akka.japi.Pair;

import java.time.Duration;

public class TokenOffsetWorker extends AbstractBehavior<TokenOffsetWorker.WorkerCmd> {
    public interface WorkerCmd {
        class GetOffset extends Pair<String, ActorRef<SetOffset>> implements WorkerCmd {
            public GetOffset(final String token, final ActorRef<SetOffset> replyTo) {
                super(token, replyTo);
            }
        }

        class SetOffset extends Pair<String, Long> implements WorkerCmd {
            public SetOffset(final Pair<String, Long> copy) {
                super(copy.first(), copy.second());
            }

            public SetOffset(final String token, final Long offset) {
                super(token, offset);
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
        return newReceiveBuilder()
                // Start off in Getting mode
                .onMessage(WorkerCmd.GetOffset.class, getOffset -> {
                    String token = getOffset.first();
                    // Get from persistence, if present tell self (if not, will be told in the future)
                    this.persistenceLayer.findOffset(token).join()
                            .ifPresent(offset -> this.getContext().getSelf()
                                    .tell(new WorkerCmd.SetOffset(token, offset)));
                    return newReceiveBuilder()
                            // Switch state to Setting mode
                            .onMessage(WorkerCmd.SetOffset.class, setOffset -> {
                                // Tell the replyTo actor the offset that has been received
                                ActorRef<WorkerCmd.SetOffset> replyTo = getOffset.second();
                                replyTo.tell(setOffset);
                                // Stop this actor
                                return Behaviors.stopped();
                            })
                            .build();
                })
                .build();
    }
}
