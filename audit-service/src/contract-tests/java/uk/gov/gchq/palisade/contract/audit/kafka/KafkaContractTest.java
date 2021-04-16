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

package uk.gov.gchq.palisade.contract.audit.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.audit.ContractTestData;
import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_ERROR_MESSAGE_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_SUCCESS_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.ERROR_RECORD_NODE_FACTORY;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.GOOD_SUCCESS_RECORD_NODE_FACTORY;

/**
 * An external requirement of the service is to connect to a pair of upstream kafka topics.
 * <ol>
 *     <li>The "error" topic can be written to by any service that encounters an error when processing a request</li>
 *     <li>The "success" topic should only be written to by the filtered-resource-service or the data-service</li>
 * </ol>
 * This service does not write to a downstream topic
 */
@SpringBootTest(
        classes = AuditApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import(KafkaInitializer.Config.class)
@ContextConfiguration(initializers = {KafkaInitializer.class})
@ActiveProfiles({"akka-test"})
class KafkaContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractTest.class);

    @Autowired
    Serializer<String> keySerialiser;
    @Autowired
    Serializer<JsonNode> valueSerialiser;
    @Autowired
    ActorSystem akkaActorSystem;
    @Autowired
    Materializer akkaMaterialiser;
    @Autowired
    KafkaContainer kafka;
    @Autowired
    AuditServiceConfigProperties auditServiceConfigProperties;

    @SpyBean
    private AuditService auditService;

    private Function<String, Integer> fileCount;

    private Supplier<Integer> currentErrorCount;
    private Supplier<Integer> currentSuccessCount;

    @BeforeEach
    void setup() {
        fileCount = (final String prefix) -> Arrays
                .stream(Objects.requireNonNull(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles()))
                .filter(file -> file.getName().startsWith(prefix))
                .collect(Collectors.toSet())
                .size();

        currentErrorCount = () -> fileCount.apply("Error");
        currentSuccessCount = () -> fileCount.apply("Success");
    }

    @AfterEach
    void tearDown() {
        Arrays.stream(Objects.requireNonNull(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles()))
                .filter(file -> (file.getName().startsWith("Success") || file.getName().startsWith("Error")))
                .peek(file -> LOGGER.info("Deleting file {}", file.getName()))
                .forEach(File::deleteOnExit);
    }

    @Test
    @DirtiesContext
    void testErrorRequestSet() throws Exception {

        // GIVEN
        // Add some messages on the error topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the
        // akka-test yaml connects the consumer to only partition 0
        var requests = ERROR_RECORD_NODE_FACTORY.get().limit(3L);

        // WHEN - we write to the input
        runStreamOf(requests);

        // THEN - check the audit service has invoked the audit method 3 times
        verify(auditService, timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodSuccessRequestSet() throws Exception {

        // GIVEN
        // Add some messages on the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(3L);

        // WHEN - we write to the input
        runStreamOf(requests);

        // THEN - check the audit service has invoked the audit method 3 times
        verify(auditService, timeout(3000).times(3)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testGoodAndBadSuccessRequestSet() throws Exception {

        // GIVEN
        // Add 2 `Good` and 2 `Bad` success messages to the success topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = Stream.of(
                GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L),
                BAD_SUCCESS_RECORD_NODE_FACTORY.get().limit(2L),
                GOOD_SUCCESS_RECORD_NODE_FACTORY.get().limit(1L))
                .flatMap(Function.identity());

        // WHEN - we write to the input
        runStreamOf(requests);

        // THEN - check the audit service has invoked the audit method for the 2 `Good` requests
        verify(auditService, timeout(3000).times(2)).audit(anyString(), any());
    }

    @Test
    @DirtiesContext
    void testFailedErrorDeserialisation() throws Exception {

        // GIVEN
        // Add a message to the 'error' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0

        var requests = BAD_ERROR_MESSAGE_NODE_FACTORY.get().limit(1L);
        var expectedErrorCount = currentErrorCount.get() + 1;

        // WHEN - we write to the input and wait
        runStreamOf(requests);

        // THEN - check an "Error-..." file has been created
        var actualErrorCount = currentErrorCount.get();
        assertThat(actualErrorCount)
                .as("Check exactly 1 'Error' file has been created")
                .isEqualTo(expectedErrorCount);
    }

    @Test
    @DirtiesContext
    void testFailedSuccessDeserialisation() throws Exception {

        // GIVEN
        // Add a message to the 'success' topic
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        var requests = ContractTestData.BAD_SUCCESS_MESSAGE_NODE_FACTORY.get().limit(1L);
        var expectedSuccessCount = currentSuccessCount.get() + 1;

        // When - we write to the input
        runStreamOf(requests);

        // Then check a "Success-..." file has been created
        var actualSuccessCount = currentSuccessCount.get();
        assertThat(actualSuccessCount)
                .as("Check exactly 1 'Success' file has been created")
                .isEqualTo(expectedSuccessCount);

    }

    private void runStreamOf(final Stream<ProducerRecord<String, JsonNode>> requests) throws InterruptedException {

        var bootstrapServers = kafka.getBootstrapServers();

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, keySerialiser, valueSerialiser)
                .withBootstrapServers(bootstrapServers);

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterialiser)
                .toCompletableFuture().join();

        TimeUnit.SECONDS.sleep(2);
    }
}
