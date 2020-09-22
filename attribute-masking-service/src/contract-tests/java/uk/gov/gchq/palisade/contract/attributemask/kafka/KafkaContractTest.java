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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, properties = "akka.discovery.config.services.kafka.from-config=false")
@Import(KafkaTestConfiguration.class)
@ActiveProfiles({"dbtest", "akkatest"})
class KafkaContractTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    // Deserialiser for downstream test error output
    static class ErrorDeserializer implements Deserializer<AuditErrorMessage> {
        @Override
        public AuditErrorMessage deserialize(final String s, final byte[] auditErrorMessage) {
            try {
                return MAPPER.readValue(auditErrorMessage, AuditErrorMessage.class);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(auditErrorMessage), e);
            }
        }
    }

    @SpyBean
    private AttributeMaskingService service;

    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ProducerTopicConfiguration producerTopicConfiguration;

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100})
    @DirtiesContext
    void testVariousRequestSets(final long messageCount) {
        // Create a variable number of requests
        final Stream<ProducerRecord<String, AttributeMaskingRequest>> requests = Stream.of(
                Stream.of(ApplicationTestData.START),
                ApplicationTestData.RECORD_FACTORY.get().limit(messageCount),
                Stream.of(ApplicationTestData.END))
                .flatMap(Function.identity());
        final long recordCount = messageCount + 2;

        // Given - the service is not mocked
        Mockito.reset(service);

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

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        // When - results are pulled from the output stream
        Probe<ConsumerRecord<String, AttributeMaskingResponse>> resultSeq = probe.request(recordCount);
        LinkedList<ConsumerRecord<String, AttributeMaskingResponse>> results = LongStream.range(0, recordCount)
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
                                        .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes()))
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
                                        .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes())),

                () -> assertThat(results.stream()
                        .map(ConsumerRecord::value)
                        .map(response -> {
                            try {
                                return response.getResource();
                            } catch (JsonProcessingException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        .map(LeafResource::getType)
                        .map(Integer::valueOf).collect(Collectors.toList()))
                        .isSorted()
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100})
    @DirtiesContext
    void testVariousRequestSetsWithErrors(final long messageCount) {
        // Create a variable number of requests
        final Stream<ProducerRecord<String, AttributeMaskingRequest>> requests = Stream.of(
                Stream.of(ApplicationTestData.START),
                ApplicationTestData.RECORD_FACTORY.get().limit(messageCount),
                Stream.of(ApplicationTestData.END))
                .flatMap(Function.identity());
        // Only expect 90% of records to be returned (excluding START/END markers)
        final long recordCount = messageCount + 2;
        final long errorCount = (long) Math.ceil(messageCount / 10.0);
        final Exception serviceSpyException = new RuntimeException("Testing error mechanism");

        // Given - the service will throw exceptions for 10% of the requests (first of each ten, so [START, message, END] -> [START, error, END])
        final AtomicLong throwCounter = new AtomicLong(0);
        Mockito.reset(service);
        Mockito.when(service.maskResourceAttributes(Mockito.argThat(obj -> throwCounter.getAndIncrement() % 10 == 0)))
                .thenThrow(serviceSpyException);

        // Given - we are already listening to the output
        ConsumerSettings<String, AttributeMaskingResponse> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ResponseDeserializer())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092");

        Probe<ConsumerRecord<String, AttributeMaskingResponse>> probe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("output-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // Given - we are already listening to the errors
        ConsumerSettings<String, AuditErrorMessage> errorConsumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092");

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                .atMostOnceSource(errorConsumerSettings, Subscriptions.topics(producerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When - we write to the input
        ProducerSettings<String, AttributeMaskingRequest> producerSettings = ProducerSettings
                .create(akkaActorSystem, new StringSerializer(), new RequestSerializer())
                .withBootstrapServers(kafkaContainer.isRunning() ? kafkaContainer.getBootstrapServers() : "localhost:9092");

        Source.fromJavaStream(() -> requests)
                .runWith(Producer.plainSink(producerSettings), akkaMaterializer);

        // When - errors are pulled from the error stream, pulling the number of errors thrown
        Probe<ConsumerRecord<String, AuditErrorMessage>> errorSeq = errorProbe.request(errorCount);
        LinkedList<ConsumerRecord<String, AuditErrorMessage>> errors = LongStream.range(0, errorCount)
                .mapToObj(i -> errorSeq.expectNext(new FiniteDuration(20 + errorCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the errors are as expected
        assertAll("Errors are correct",
                // The correct number of errors were received
                () -> assertThat(errors)
                        .hasSize((int) errorCount),

                () -> assertThat(errors)
                        .allSatisfy(error ->
                                assertAll("Error records all satisfy requirements",
                                        // All messages have the original request preserved
                                        () -> assertThat(error.headers().lastHeader(Token.HEADER).value())
                                                .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes()),

                                        // The original request was preserved
                                        () -> assertThat(error.value().getUserId())
                                                .isEqualTo(ApplicationTestData.USER_ID.getId()),

                                        () -> assertThat(error.value().getResourceId())
                                                .isEqualTo(ApplicationTestData.RESOURCE_ID),

                                        () -> assertThat(error.value().getContext())
                                                .isEqualTo(ApplicationTestData.CONTEXT),

                                        // The error was reported successfully
                                        () -> assertThat(error.value().getError().getMessage())
                                                .isEqualTo(serviceSpyException.getMessage())
                                )
                        )
        );

        // When - results are pulled from the output stream, pulling only 90% of the total
        Probe<ConsumerRecord<String, AttributeMaskingResponse>> resultSeq = probe.request(recordCount - errorCount);
        LinkedList<ConsumerRecord<String, AttributeMaskingResponse>> results = LongStream.range(0, recordCount - errorCount)
                .mapToObj(i -> resultSeq.expectNext(new FiniteDuration(20 + recordCount, TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then - the results are as expected, considering the errors that were thrown
        // All messages have a correct Token in the header
        assertAll("Headers have correct token",
                () -> assertThat(results)
                        .hasSize((int) (recordCount - errorCount)),

                () -> assertThat(results)
                        .allSatisfy(result ->
                                assertThat(result.headers().lastHeader(Token.HEADER).value())
                                        .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes()))
        );

        // The first and last have a correct StreamMarker header
        assertAll("StreamMarkers are correct START and END",
                () -> assertThat(results.getFirst().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.START.toString().getBytes()),

                () -> assertThat(results.getLast().headers().lastHeader(StreamMarker.HEADER).value())
                        .isEqualTo(StreamMarker.END.toString().getBytes())
        );

        // The correct number of results were returned
        results.removeFirst();
        results.removeLast();
        assertThat(results)
                .hasSize((int) (messageCount - errorCount));
    }

}
