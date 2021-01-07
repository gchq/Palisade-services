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

import akka.actor.ActorRef;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.typed.ActorSystem;
import akka.japi.Pair;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.filteredresource.ApplicationTestData;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand.AckTellWorker;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand.SpawnWorker;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetWorker.SetOffset;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetWorker.WorkerResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TokenOffsetControllerTest {
    private TokenOffsetPersistenceLayer persistenceLayer;
    private ActorSystem<TokenOffsetCommand> actorSystem;
    private TestInbox<WorkerResponse> replyInbox;
    private TestInbox<SetOffset> ackInbox;
    private SpawnWorker spawnCommand;
    private SetOffset setCommand;
    private TokenOffsetCommand.AckTellWorker ackTellCommand;

    @BeforeEach
    void setUp() {
        // Set up worker and testkit actors
        persistenceLayer = new MapTokenOffsetPersistenceLayer();
        actorSystem = (ActorSystem<TokenOffsetCommand>) TokenOffsetController.create(persistenceLayer);
        replyInbox = TestInbox.create();
        ackInbox = TestInbox.create();

        // Set up worker commands
        spawnCommand = new SpawnWorker(ApplicationTestData.REQUEST_TOKEN, replyInbox.getRef());
        setCommand = new SetOffset(ApplicationTestData.REQUEST_TOKEN, ApplicationTestData.OFFSET);
        ackTellCommand = new AckTellWorker(Pair.create(setCommand.token, setCommand.offset), ackInbox.getRef());
    }

    @Test
    void testCorrectCommandOrderWithoutPersistence() throws InterruptedException {
        // Given we have an actor-system running

        // When a spawn command is sent
        actorSystem.tell(spawnCommand);
        TimeUnit.MILLISECONDS.sleep(50);

        // Then a worker actor for our request is spawned
        Optional<ActorRef> workerRef = actorSystem.classicSystem()
                .actorSelection("/user/" + spawnCommand.getOffset.token)
                .resolveOne(Duration.ofSeconds(1L))
                .thenApply(Optional::of)
                .exceptionally(ex -> Optional.empty())
                .toCompletableFuture().join();
        assertThat(workerRef).isPresent();

        // When a tell command is sent
        actorSystem.tell(ackTellCommand);
        TimeUnit.MILLISECONDS.sleep(50);

        // When the actor-system is drained and shut-down
        TestKit.shutdownActorSystem(actorSystem.classicSystem());

        // Then the message was acked
        assertThat(ackInbox.getAllReceived())
                .hasSize(1)
                .first().isEqualTo(ackTellCommand.setOffset);

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().usingRecursiveComparison().isEqualTo(setCommand);
    }

    @Test
    void testCorrectCommandOrderWithPersistence() throws InterruptedException {
        // Given we have an actor-system running
        // Given the offset is in persistence
        persistenceLayer.overwriteOffset(spawnCommand.getOffset.token, setCommand.offset);

        // When a spawn command is sent
        actorSystem.tell(spawnCommand);
        TimeUnit.MILLISECONDS.sleep(50);

        // When the actor-system is drained and shut-down
        TestKit.shutdownActorSystem(actorSystem.classicSystem());

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().usingRecursiveComparison().isEqualTo(setCommand);
    }
}
