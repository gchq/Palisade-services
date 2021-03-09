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

import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.typed.Behavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.filteredresource.ApplicationTestData;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.DeregisterWorker;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.GetOffset;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.SetOffset;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.WorkerCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetWorker.WorkerResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TokenOffsetWorkerTest {
    private TokenOffsetPersistenceLayer persistenceLayer;
    private Behavior<WorkerCommand> worker;
    private TestInbox<WorkerResponse> replyInbox;
    private GetOffset getCommand;
    private SetOffset setCommand;

    @BeforeEach
    void setUp() {
        // Set up worker and testkit actors
        persistenceLayer = new MapTokenOffsetPersistenceLayer();
        final TestInbox<DeregisterWorker> parentInbox = TestInbox.create();
        worker = TokenOffsetWorker.create(persistenceLayer, parentInbox.getRef());

        // Set up worker commands
        replyInbox = TestInbox.create();
        getCommand = new GetOffset(ApplicationTestData.REQUEST_TOKEN, replyInbox.getRef());
        setCommand = new SetOffset(ApplicationTestData.REQUEST_TOKEN, ApplicationTestData.OFFSET);
    }

    @Test
    void testCorrectCommandOrderWithoutPersistence() {
        // Given we have a worker running
        BehaviorTestKit<WorkerCommand> testKit = BehaviorTestKit.create(worker);

        // When a get command is sent - persistence is empty so it will wait for a SetCommand externally
        testKit.run(getCommand);
        // When a set command is sent
        testKit.run(setCommand);

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().isEqualTo(setCommand);
        // Then the worker dies having completed its job
        assertThat(testKit.isAlive()).isFalse();
    }

    @Test
    void testCorrectCommandOrderWithPersistence() {
        // Given
        BehaviorTestKit<WorkerCommand> testKit = BehaviorTestKit.create(worker);
        persistenceLayer.overwriteOffset(setCommand.token, setCommand.offset);

        // When a get command is sent - token-offset pair is in persistence so will tell itself a SetCommand message
        testKit.run(getCommand);
        // When it tells itself its own SetCommand from persistence
        testKit.runOne();

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().usingRecursiveComparison().isEqualTo(setCommand);
        // Then the worker dies having completed its job
        assertThat(testKit.isAlive()).isFalse();
    }

    @Test
    void testEarlySetCommand() {
        // Given
        BehaviorTestKit<WorkerCommand> testKit = BehaviorTestKit.create(worker);
        persistenceLayer.overwriteOffset(setCommand.token, setCommand.offset);
        Behavior<WorkerCommand> initialBehavior = testKit.currentBehavior();

        // When a set command is sent (too early)
        testKit.run(setCommand);

        // Then it is dropped without affecting the actor behavior
        assertThat(testKit.currentBehavior()).isEqualTo(initialBehavior);

        // The rest of the worker's lifecycle should execute as expected
        // When a get command is sent - persistence is empty so it will wait for a SetCommand externally
        testKit.run(getCommand);
        // When a set command is sent
        testKit.run(setCommand);

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().isEqualTo(setCommand);
        // Then the worker dies having completed its job
        assertThat(testKit.isAlive()).isFalse();
    }

    @Test
    void testDoubleGetCommand() {
        // Given
        BehaviorTestKit<WorkerCommand> testKit = BehaviorTestKit.create(worker);
        persistenceLayer.overwriteOffset(setCommand.token, setCommand.offset);
        Behavior<WorkerCommand> initialBehavior = testKit.currentBehavior();

        // When a get command is sent twice
        testKit.run(getCommand);
        Behavior<WorkerCommand> behaviorAfterFirstGet = testKit.currentBehavior();
        testKit.run(getCommand);

        // Then the first get affects the worker behavior as expected
        assertThat(behaviorAfterFirstGet).isNotEqualTo(initialBehavior);
        // Then the second get is dropped without affecting the actor behavior
        assertThat(testKit.currentBehavior()).isEqualTo(behaviorAfterFirstGet);

        // The rest of the worker's lifecycle should execute as expected
        // When a set command is sent
        testKit.run(setCommand);

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().isEqualTo(setCommand);
        // Then the worker dies having completed its job
        assertThat(testKit.isAlive()).isFalse();
    }

    @Test
    void testSetForIncorrectToken() {
        // Given we have a worker running
        BehaviorTestKit<WorkerCommand> testKit = BehaviorTestKit.create(worker);

        // When a get command is sent - persistence is empty so it will wait for a SetCommand externally
        testKit.run(getCommand);
        Behavior<WorkerCommand> behaviorAfterGet = testKit.currentBehavior();
        // When a set command is sent for the wrong token
        testKit.run(new SetOffset("different-token-to-" + setCommand.token, setCommand.offset));

        // Then it is ignored and the behavior is unchanged
        assertThat(replyInbox.getAllReceived())
                .isEmpty();
        assertThat(testKit.currentBehavior()).isEqualTo(behaviorAfterGet);

        // When the correct set command is sent
        testKit.run(setCommand);

        // Then the set arrives in the replyTo inbox
        assertThat(replyInbox.getAllReceived())
                .hasSize(1)
                .first().isEqualTo(setCommand);
        // Then the worker dies having completed its job
        assertThat(testKit.isAlive()).isFalse();
    }
}
