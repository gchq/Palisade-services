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

package uk.gov.gchq.palisade.contract.attributemask.akka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
class KafkaContractTest {

    public static class AttributeMaskingRequestSerialiser extends JsonSerializer<AttributeMaskingRequest> {
    }

    public static class AttributeMaskingResponseDeserialiser extends JsonDeserializer<AttributeMaskingResponse> {
    }

    public static <R> CompletableFuture<LinkedList<ConsumerRecord<String, R>>> consumeWithTimeout(final KafkaConsumer<String, R> consumer, final Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
                    LinkedList<ConsumerRecord<String, R>> collected = new LinkedList<>();
                    consumer.poll(timeout).forEach(collected::add);
                    return collected;
                }
        );
    }

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.5.1");
    private static Map<String, Object> producerProperties;
    private static Map<String, Object> consumerProperties;
    private static final Duration TIMEOUT_SECONDS = Duration.ofSeconds(15);

    @BeforeAll
    static void startTestcontainers() {
        KAFKA_CONTAINER.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        KAFKA_CONTAINER.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        KAFKA_CONTAINER.start();

        Map<String, Object> adminClientProperties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(adminClientProperties)) {
            List<NewTopic> newTopicsConfig = List.of(
                    new NewTopic("rule", 3, (short) 1),
                    new NewTopic("filtered-resource", 3, (short) 1));
            adminClient.createTopics(newTopicsConfig);
        }

        producerProperties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AttributeMaskingRequestSerialiser.class);
        consumerProperties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.GROUP_ID_CONFIG, "test.group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AttributeMaskingResponseDeserialiser.class);
    }

    @AfterAll
    static void stopTestcontainers() {
        KAFKA_CONTAINER.stop();
    }

    @Test
    @Disabled
    void sendAndReceiveTest() throws ExecutionException, InterruptedException {
        // Given a message is available on the upstream topic
        try (KafkaProducer<String, AttributeMaskingRequest> producer = new KafkaProducer<>(producerProperties)) {
            producer.send(ApplicationTestData.START);
            producer.send(ApplicationTestData.RECORD);
            producer.send(ApplicationTestData.END);
        }

        // When we read all messages from the downstream topic
        LinkedList<ConsumerRecord<String, AttributeMaskingResponse>> resultsList;
        try (KafkaConsumer<String, AttributeMaskingResponse> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(Collections.singletonList("filtered-resource"));
            resultsList = consumeWithTimeout(consumer, TIMEOUT_SECONDS).get();
        }

        // Then there are three messages: START, RESPONSE, END
        assertThat(resultsList)
                .hasSize(3);

        // Then the first message is a START stream marker
        // Then the last message is a END stream marker
        assertThat(List.of(resultsList.getFirst(), resultsList.getLast()))
                .extracting(ConsumerRecord::headers)
                .isEqualTo(List.of(ApplicationTestData.START.headers(), ApplicationTestData.END.headers()));
        resultsList.removeFirst();
        resultsList.removeLast();

        // Then all middle messages are mapped from request to response appropriately
        assertThat(resultsList)
                .extracting(ConsumerRecord::value)
                .hasSize(1)
                .containsExactly(ApplicationTestData.RESPONSE);
    }

}

