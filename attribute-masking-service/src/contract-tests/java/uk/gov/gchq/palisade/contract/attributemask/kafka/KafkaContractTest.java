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

package uk.gov.gchq.palisade.contract.attributemask.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.attributemask.ContractTestData;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer.RequestSerializer;
import uk.gov.gchq.palisade.contract.attributemask.KafkaInitializer.ResponseDeserializer;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.common.Token;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.SerDesConfig;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "rule" topic is written to by the policy-service and read by this service.
 * The downstream "filtered-resource" topic is written to by this service and read by the filtered-resource-service.
 * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
 */
@SpringBootTest(
        classes = AttributeMaskingApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "akka.discovery.config.services.kafka.from-config=false"
)
@Import({KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {KafkaInitializer.class})
@ActiveProfiles({"db-test", "akka-test"})
class KafkaContractTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100})
    @DirtiesContext
    void testVariousRequestSets(final long messageCount) {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akkatest yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.RECORD_NODE_FACTORY.get().limit(messageCount),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());
        final long recordCount = messageCount + 2;

        // Given - we are already listening to the output
        ConsumerSettings<String, JsonNode> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, JsonNode>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers());

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, JsonNode>> resultSeq = probe.request(recordCount);
        LinkedList<ConsumerRecord<String, JsonNode>> results = LongStream.range(0, recordCount)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + recordCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected

        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .hasSize((int) recordCount),

                () -> assertThat(results)
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("StreamMarkers are correct START and END",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // All but the first and last have the expected message
        results.removeFirst();
        results.removeLast();
        assertAll("Results are correct and ordered",
                () -> assertThat(results)
                        .hasSize((int) messageCount),

                () -> assertThat(results)
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes())),

                () -> assertThat(results.stream()
                        .map(ConsumerRecord::value)
                        .map(response -> response.get("resource").get("type").asInt())
                        .collect(Collectors.toList()))
                        .isSorted()
        );
    }

    @Test
    @DirtiesContext
    void testRestEndpoint() {
        // Given - we are already listening to the service input
        ConsumerSettings<String, AttributeMaskingRequest> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, SerDesConfig.ruleKeyDeserializer(), SerDesConfig.ruleValueDeserializer())
                .withGroupId("test-group")
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AttributeMaskingRequest>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("input-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<AttributeMaskingRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/mask", entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, AttributeMaskingRequest>> resultSeq = probe.request(1);
        LinkedList<ConsumerRecord<String, AttributeMaskingRequest>> results = LongStream.range(0, 1)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        // The request was written with the correct header
        assertAll("Records returned are correct",
                () -> assertThat(results)
                        .hasSize(1),

                () -> assertThat(results)
                        .allSatisfy(result -> {
                            assertThat(result.headers().lastHeader(Token.HEADER).value())
                                    .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());

                            assertThat(result.value())
                                    .isEqualTo(ContractTestData.REQUEST_OBJ);
                        })
        );
    }
}
