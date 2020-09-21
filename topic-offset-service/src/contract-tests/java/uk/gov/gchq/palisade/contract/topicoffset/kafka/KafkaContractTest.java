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
package uk.gov.gchq.palisade.contract.topicoffset.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.topicoffset.TopicOffsetApplication;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.gchq.palisade.contract.topicoffset.kafka.ContractTestConfiguration.END;
import static uk.gov.gchq.palisade.contract.topicoffset.kafka.ContractTestConfiguration.RECORD;
import static uk.gov.gchq.palisade.contract.topicoffset.kafka.ContractTestConfiguration.START;
import static uk.gov.gchq.palisade.contract.topicoffset.kafka.ContractTestConfiguration.consumeWithTimeout;


/**
     * An external requirement of the service is to connect to a pair of kafka topics.
     * The upstream "rule" topic is written to by the policy-service and read by this service.
     * The downstream "filtered-resource" topic is written to by this service and read by the filtered-resource-service.
     * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
     */
    @Disabled
    @SpringBootTest(classes = TopicOffsetApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dbtest")
    class KafkaContractTest {

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
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ContractTestConfiguration.TopicOffsetRequestSerialiser.class);
                consumerProperties = Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.GROUP_ID_CONFIG, "test.group",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ContractTestConfiguration.TopicOffsetResponseDeserialiser.class);
            }

        public static final TopicOffsetResponse RESPONSE = TopicOffsetResponse.Builder.create().withOffset(1023L);

            @AfterAll
            static void stopTestcontainers() {
                KAFKA_CONTAINER.stop();
            }

            @Test
            void sendAndReceiveTest() throws ExecutionException, InterruptedException {
                // Given a message is available on the upstream topic
                try (KafkaProducer<String, TopicOffsetRequest> producer = new KafkaProducer<>(producerProperties)) {
                    producer.send(START);
                    producer.send(RECORD);
                    producer.send(END);
                }

                // When we read all messages from the downstream topic
                LinkedList<ConsumerRecord<String, TopicOffsetResponse>> resultsList;
                try (KafkaConsumer<String, TopicOffsetResponse> consumer = new KafkaConsumer<>(consumerProperties)) {
                    consumer.subscribe(Collections.singletonList("filtered-resource"));
                    resultsList = consumeWithTimeout(consumer, TIMEOUT_SECONDS).get();
                }

                // Then there are three messages: START, RESPONSE, END
                assertThat(resultsList)
                        .hasSize(3);

                // Then the first message is a START stream marker
                assertThat(resultsList.getFirst().headers())
                        .isEqualTo(START.headers());
                resultsList.removeFirst();

                // Then the last message is a END stream marker
                assertThat(resultsList.getLast().headers())
                        .isEqualTo(END.headers());
                resultsList.removeLast();

                // Then all middle messages are mapped from request to response appropriately
                assertThat(resultsList)
                        .extracting(ConsumerRecord::value)
                        .hasSize(1)
                        .containsExactly(RESPONSE);
            }

        }
