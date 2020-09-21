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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An external requirement of the service is to connect to a pair of kafka topics.
 * The upstream "rule" topic is written to by the policy-service and read by this service.
 * The downstream "filtered-resource" topic is written to by this service and read by the filtered-resource-service.
 * Upon writing to the upstream topic, appropriate messages should be written to the downstream topic.
 */
@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, properties = "akka.discovery.config.services.kafka.from-config=false")
@Import(KafkaTestConfiguration.class)
@ActiveProfiles({"dbtest", "akkatest"})
class KafkaContractTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractTest.class);

    // Serialiser for upstream test input
    static class RequestSerializer implements Serializer<AttributeMaskingRequest> {
        @Override
        public byte[] serialize(final String s, final AttributeMaskingRequest attributeMaskingRequest) {
            try {
                return MAPPER.writeValueAsBytes(attributeMaskingRequest);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException("Failed to serialize " + attributeMaskingRequest.toString(), e);
            }
        }
    }

    // Deserialiser for downstream test output
    static class ResponseDeserializer implements Deserializer<AttributeMaskingResponse> {
        @Override
        public AttributeMaskingResponse deserialize(final String s, final byte[] attributeMaskingResponse) {
            try {
                return MAPPER.readValue(attributeMaskingResponse, AttributeMaskingResponse.class);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(attributeMaskingResponse), e);
            }
        }
    }

    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100, 1000, 10000})
    @DirtiesContext
    void testVariousRequestSets(final long requestCount) {
        // Create a variable number of requests
        final Stream<ProducerRecord<String, AttributeMaskingRequest>> requests = Stream
                .of(
                        Stream.of(ApplicationTestData.START),
                        ApplicationTestData.RECORD_FACTORY.get().limit(requestCount),
                        Stream.of(ApplicationTestData.END)
                )
                .flatMap(Function.identity());
        final long recordCount = requestCount + 2;

        // Given - we are already listening to the output
        ConsumerSettings<String, AttributeMaskingResponse> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserializer())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092");

        Probe<ConsumerRecord<String, AttributeMaskingResponse>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        ProducerSettings<String, AttributeMaskingRequest> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092");

        Source.fromIterator(requests::iterator)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, AttributeMaskingResponse>> resultSeq = probe.request(recordCount);
        LinkedList<ConsumerRecord<String, AttributeMaskingResponse>> results = LongStream.range(0, recordCount)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + recordCount, TimeUnit.SECONDS)))
                .peek(record -> LOGGER.info("Probe received consumer record: {}", record))
                .collect(Collectors.toCollection(LinkedList::new));

        assertThat(results).hasSize((int) recordCount);
    }

}
