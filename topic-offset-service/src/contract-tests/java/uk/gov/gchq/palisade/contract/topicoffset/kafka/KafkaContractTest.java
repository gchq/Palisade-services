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

package uk.gov.gchq.palisade.contract.topicoffset.kafka;

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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.topicoffset.ContractTestData;
import uk.gov.gchq.palisade.contract.topicoffset.kafka.KafkaTestConfiguration.RequestSerialiser;
import uk.gov.gchq.palisade.contract.topicoffset.kafka.KafkaTestConfiguration.ResponseDeserialiser;
import uk.gov.gchq.palisade.service.topicoffset.TopicOffsetApplication;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.topicoffset.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.topicoffset.stream.SerDesConfig;

import java.nio.charset.Charset;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "masked-resource" topic is written to by the Attribute-Masking Service and read by this service.
 * The downstream "masked-resource-offset" topic is written to by this service and read by the Filtered-Resource Service.
 * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
 */
@SpringBootTest(
        classes = TopicOffsetApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import(KafkaTestConfiguration.class)
@ActiveProfiles({"akka-test", "testcontainers"})
@SuppressWarnings("java:S1774")
        //Suppress ternary operator smell
class KafkaContractTest {
    private static final String KAFKA_URL = "localhost:9092";
    private static final Integer TIMEOUT = 20;

    @SpyBean
    private TopicOffsetService service;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterialiser;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100})
    @DirtiesContext
    void testVariousRequestSets(final long messageCount) {
        // Create a variable number of requests
        // The ContractTestData.REQUEST_TOKEN maps to partition 0 of [0, 1, 2], so the akka-test yaml connects the consumer to only partition 0
        final Stream<ProducerRecord<String, JsonNode>> requests = Stream.of(
                Stream.of(ContractTestData.START_RECORD),
                ContractTestData.RECORD_NODE_FACTORY.get().limit(messageCount),
                Stream.of(ContractTestData.END_RECORD))
                .flatMap(Function.identity());
        long recordCount = 1; // We expect only the "START" record

        // Given - the service is not mocked
        Mockito.reset(service);

        // Given - we are already listening to the output
        ConsumerSettings<String, JsonNode> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserialiser())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : KAFKA_URL)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, JsonNode>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);

        // When - we write to the input
        ProducerSettings<String, JsonNode> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerialiser())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : KAFKA_URL);

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterialiser)
                .toCompletableFuture().join();

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, JsonNode>> resultSeq = probe.request(recordCount);
        LinkedList<ConsumerRecord<String, JsonNode>> results = LongStream.range(0, recordCount)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(TIMEOUT + recordCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        // All messages have a correct Token in the header
        assertThat(results)
                .as("Check that %s messages are returned", (int) recordCount)
                .hasSize((int) recordCount)
                .allSatisfy(result -> assertAll("Test each message",
                        () -> assertThat(result.headers().lastHeader(Token.HEADER).value())
                                .as("Check that the token is in the header")
                                .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes()),

                        () -> assertThat(result.headers().lastHeader(StreamMarker.HEADER).value())
                                .as("Check that the Start of stream message has been observed")
                                .isEqualTo(StreamMarker.START.toString().getBytes(Charset.defaultCharset())),

                        () -> assertThat(result.value()).isNotNull()
                ));

        // Then - there are no more messages
        var noResultSeq = probe.request(1);
        var timeout = new FiniteDuration(2, TimeUnit.SECONDS);
        assertThrows(AssertionError.class, () -> noResultSeq.expectNext(timeout), "An exception will be thrown after no more results within 2 seconds");
    }

    @Test
    @DirtiesContext
    void testRestEndpoint() {
        // Given - we are already listening to the service input
        ConsumerSettings<String, TopicOffsetRequest> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, SerDesConfig.maskedResourceKeyDeserialiser(), SerDesConfig.maskedResourceValueDeserialiser())
                .withGroupId("test-group")
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : KAFKA_URL)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, TopicOffsetRequest>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("input-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterialiser);

        // When - we POST to the rest endpoint
        Map<String, List<String>> headers = Collections.singletonMap(Token.HEADER, Collections.singletonList(ContractTestData.REQUEST_TOKEN));
        HttpEntity<TopicOffsetRequest> entity = new HttpEntity<>(ContractTestData.REQUEST_OBJ, new LinkedMultiValueMap<>(headers));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/offset", entity, Void.class);

        // Then - the REST request was accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, TopicOffsetRequest>> resultSeq = probe.request(1);
        LinkedList<ConsumerRecord<String, TopicOffsetRequest>> results = LongStream.range(0, 1)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(TIMEOUT, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected
        // The request was written with the correct header
        assertThat(results)
                .as("Check that one message is returned")
                .hasSize(1)
                .allSatisfy((var result) -> {
                    assertThat(result.headers().lastHeader(Token.HEADER).value())
                            .as("Check that the token is in the header")
                            .isEqualTo(ContractTestData.REQUEST_TOKEN.getBytes());

                    assertThat(result.value())
                            .as("Check that the requestObject returned is the same as the one we passed into the service")
                            .usingRecursiveComparison()
                            .isEqualTo(ContractTestData.REQUEST_OBJ);
                });
    }

}
