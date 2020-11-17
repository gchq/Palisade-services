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

package uk.gov.gchq.palisade.component.filteredresource.repository;

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetActorSystem;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetActorSystem.TokenOffsetCmd;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class TokenOffsetActorSystemTest {
    public static void main(final String[] args) throws InterruptedException {
        // Setup a persistence layer
        TokenOffsetPersistenceLayer persistenceLayer = new MapTokenOffsetPersistenceLayer();
        persistenceLayer.overwriteOffset("five", 5L);

        // Start the tokenOffset system
        akka.actor.ActorSystem actorSystem = akka.actor.ActorSystem.create();
        ActorSystem<TokenOffsetCmd> tokenOffsetSystem = TokenOffsetActorSystem.create(persistenceLayer);

        // Run the system with two tokens to request offsets for
        // Only one of these is in the persistence layer so far
        Source.fromJavaStream(() -> Stream.of("five", "six"))
                .via(TokenOffsetActorSystem.asGetterFlow(tokenOffsetSystem))
                .to(Sink.foreach(toldOffset -> System.out.println(toldOffset.first() + " :: " + toldOffset.second().toString())))
                .run(actorSystem);

        // Add the second token and its offset to persistence, allow some time for async processing
        Source.fromJavaStream(() -> Stream.of(new Pair<>("six", 6L)))
                .mapAsync(2, pair -> persistenceLayer.putOffsetIfAbsent(pair.first(), pair.second())
                        .thenApply(ign -> pair))
                .to(TokenOffsetActorSystem.asSetterSink(tokenOffsetSystem))
                .run(actorSystem);

        TimeUnit.SECONDS.sleep(1L);
    }
}
