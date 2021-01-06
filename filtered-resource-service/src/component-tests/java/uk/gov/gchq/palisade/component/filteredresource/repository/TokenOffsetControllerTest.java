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

import akka.actor.typed.ActorRef;
import akka.japi.Pair;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.filteredresource.model.TokenOffsetPersistenceResponse;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TokenOffsetControllerTest {


    @Test
    void testOffsetSystemReturnsEarlyAndLateValues() throws InterruptedException, ExecutionException, TimeoutException {
        // Setup a persistence layer
        TokenOffsetPersistenceLayer persistenceLayer = new MapTokenOffsetPersistenceLayer();
        persistenceLayer.overwriteOffset("five", 5L);

        // Start the tokenOffset system
        akka.actor.ActorSystem actorSystem = akka.actor.ActorSystem.create();
        ActorRef<TokenOffsetCommand> tokenOffsetSystem = TokenOffsetController.create(persistenceLayer);

        // Run the system with two tokens to request offsets for
        // Only one of these is in the persistence layer so far
        CompletableFuture<List<Pair<String, Long>>> messages = Source.fromJavaStream(() -> Stream.of("five", "six", "seven"))
                .via(TokenOffsetController.asGetterFlow(tokenOffsetSystem))
                // Filter out and ignore errors
                .filter(response -> response.getException() == null)
                // Extract successes
                .map(response -> Pair.create(response.getToken(), response.getOffset()))
                // Run and materialize to list of (successful) responses
                .runWith(Sink.seq(), actorSystem)
                .toCompletableFuture();

        // Add the second token and its offset to persistence
        Source.fromJavaStream(() -> Stream.of(new Pair<>("seven", 7L), new Pair<>("six", 6L)))
                .mapAsync(2, pair -> persistenceLayer.putOffsetIfAbsent(pair.first(), pair.second())
                        .thenApply(ign -> pair))
                .to(TokenOffsetController.asSetterSink(tokenOffsetSystem))
                .run(actorSystem);

        // Assert that we received all the expected messages
        assertThat(messages.get(1, TimeUnit.SECONDS))
                .hasSize(3)
                .isEqualTo(List.of(Pair.create("five", 5L), Pair.create("six", 6L), Pair.create("seven", 7L)));
    }

    @Test
    void testOffsetSystemWithBadPersistence() throws InterruptedException, ExecutionException, TimeoutException {
        // Given - the persistence layer will throw errors on "six"es
        Throwable extrudedException = new RuntimeException("Persistence Layer forcibly failed for testing scenario");
        TokenOffsetPersistenceLayer persistenceLayer = Mockito.spy(new MapTokenOffsetPersistenceLayer());
        persistenceLayer.overwriteOffset("five", 5L);
        Mockito.doReturn(CompletableFuture.failedFuture(extrudedException))
                .when(persistenceLayer).findOffset("six");

        // Start the tokenOffset system
        akka.actor.ActorSystem actorSystem = akka.actor.ActorSystem.create();
        ActorRef<TokenOffsetCommand> tokenOffsetSystem = TokenOffsetController.create(persistenceLayer);

        // Run the system with two tokens to request offsets for
        // Only one of these is in the persistence layer so far
        CompletableFuture<List<TokenOffsetPersistenceResponse>> messages = Source.fromJavaStream(() -> Stream.of("five", "six", "seven"))
                .via(TokenOffsetController.asGetterFlow(tokenOffsetSystem))
                // Run and materialize to list of (successful) responses
                .runWith(Sink.seq(), actorSystem)
                .toCompletableFuture();

        // Add the second token and its offset to persistence
        Source.fromJavaStream(() -> Stream.of(new Pair<>("seven", 7L)/*, new Pair<>("six", 6L)*/))
                .mapAsync(2, pair -> persistenceLayer.putOffsetIfAbsent(pair.first(), pair.second())
                        .thenApply(ign -> pair))
                .to(TokenOffsetController.asSetterSink(tokenOffsetSystem))
                .run(actorSystem);

        // Assert that we received all the expected messages
        assertThat(messages.get(1, TimeUnit.SECONDS))
                .hasSize(3)
                .extracting(TokenOffsetPersistenceResponse::getOffset)
                .usingRecursiveComparison()
                .isEqualTo(Stream.of(5L, null, 7L).collect(Collectors.toList()));

        assertThat(messages.get())
                .extracting(TokenOffsetPersistenceResponse::getException)
                .usingRecursiveComparison()
                .isEqualTo(Stream.of(null, extrudedException, null).collect(Collectors.toList()));
    }

}
