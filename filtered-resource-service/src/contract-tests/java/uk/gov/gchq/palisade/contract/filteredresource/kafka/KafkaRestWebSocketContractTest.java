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

package uk.gov.gchq.palisade.contract.filteredresource.kafka;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.japi.Pair;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import scala.concurrent.duration.FiniteDuration;

import uk.gov.gchq.palisade.contract.filteredresource.common.ContractTestData.ParameterizedArguments;
import uk.gov.gchq.palisade.contract.filteredresource.common.ContractTestData.ParameterizedArgumentsNoResources;
import uk.gov.gchq.palisade.contract.filteredresource.kafka.KafkaInitializer.ErrorDeserializer;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(
        classes = FilteredResourceApplication.class,
        webEnvironment = WebEnvironment.NONE,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import({KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {KafkaInitializer.class, RedisInitializer.class})
@ActiveProfiles({"k8s", "akka"})
class KafkaRestWebSocketContractTest {
    private static final String HOST = "localhost";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${server.port}")
    private Integer port;
    @Autowired
    private TokenOffsetPersistenceLayer persistenceLayer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;

    // Handle serializing Objects to JSON Strings
    private static <T> T deserialize(final String json, final Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write message", e);
        }
    }

    // Handle serializing Objects to JSON Strings
    private static String serialize(final Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write message", e);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ParameterizedArguments.class)
    void testAkkaRunnableGraph(
            // Parameterised input
            final String requestToken,
            final List<Pair<Iterable<HttpHeader>, FilteredResourceRequest>> maskedResourceTopic,
            final List<Pair<Iterable<HttpHeader>, TopicOffsetMessage>> maskedResourceOffsetTopic,
            final Map<String, Long> offsetsPersistence,
            final List<WebSocketMessage> websocketRequests,
            // Expected output
            final List<WebSocketMessage> websocketResponses,
            final List<AuditErrorMessage> auditErrorMessages
    ) throws InterruptedException, ExecutionException, TimeoutException {

        ContentType jsonType = ContentTypes.APPLICATION_JSON;
        Http http = Http.get(akkaActorSystem);

        // Given
        // POST maskedResource to KafkaController
        maskedResourceTopic.forEach(resource -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource", HOST, port))
                        .withHeaders(resource.first())
                        .withEntity(jsonType, serialize(resource.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Given POST maskedResourceOffset to KafkaController - runnable is then called 'late' to simulate arrival of offsets *after* client request
        Runnable postOffsets = () -> maskedResourceOffsetTopic.forEach(offset -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource-offset", HOST, port))
                        .withHeaders(offset.first())
                        .withEntity(jsonType, serialize(offset.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Write offsets to persistence
        offsetsPersistence.forEach((token, offset) -> persistenceLayer
                .overwriteOffset(token, offset)
                .join());

        // When
        // Send each websocketMessage request and receive responses
        Source<Message, NotUsed> wsMsgSource = Source.fromIterator(websocketRequests::iterator).map(this::writeTextMessage);
        Sink<Message, CompletionStage<List<WebSocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebSocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebSocketMessage>>> request = http.singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%s/resource/" + requestToken, HOST, port)),
                clientFlow,
                akkaMaterializer);
        // Get the (HTTP) response, a websocket upgrade
        request.first().toCompletableFuture()
                .get(1, TimeUnit.SECONDS);

        // Late POST of offsets after client request has been initialized
        postOffsets.run();

        // Get the result of the client sink, a list of (WebSocket) responses
        LinkedList<WebSocketMessage> actualResponses = new LinkedList<>(request.second().toCompletableFuture()
                .get(30 + websocketRequests.size(), TimeUnit.SECONDS));

        // Then
        // Assert each received response matches up with the expected
        assertThat(actualResponses)
                .as("Testing Websocket Response")
                .isEqualTo(websocketResponses);

        if (auditErrorMessages.size() > 0) {

            ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                    .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                    .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                    .withGroupId("error-topic-test-consumer")
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                    .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("error-topic").getName()))
                    .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);


            // When you read off the error queue
            LinkedList<ConsumerRecord<String, AuditErrorMessage>> errorResults = LongStream.range(0, auditErrorMessages.size())
                    .mapToObj(i -> errorProbe.requestNext(FiniteDuration.create(20 + auditErrorMessages.size(), TimeUnit.SECONDS)))
                    .collect(Collectors.toCollection(LinkedList::new));

            // Then
            // The messages on the error topic are as expected
            assertAll("Asserting on the error topic",
                    // One error is produced
                    () -> assertThat(errorResults)
                            .as("Assert that there is one error on the error topic")
                            .hasSize(1),

                    () -> assertThat(errorResults.get(0).value())
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(Throwable.class)
                            .ignoringFields("timestamp")
                            .isEqualTo(auditErrorMessages.get(0))
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ParameterizedArgumentsNoResources.class)
    @Disabled
    void testNoResourcesObservedException(
            // Parameterised input
            final String requestToken,
            final List<Pair<Iterable<HttpHeader>, FilteredResourceRequest>> maskedResourceTopic,
            final List<Pair<Iterable<HttpHeader>, TopicOffsetMessage>> maskedResourceOffsetTopic,
            final List<WebSocketMessage> websocketRequests,
            // Expected output
            final List<WebSocketMessage> websocketResponses,
            final List<AuditErrorMessage> auditErrorMessages
    ) throws InterruptedException, ExecutionException, TimeoutException {

        ContentType jsonType = ContentTypes.APPLICATION_JSON;
        Http http = Http.get(akkaActorSystem);

        // Given
        // POST maskedResource to KafkaController
        maskedResourceTopic.forEach(resource -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource", HOST, port))
                        .withHeaders(resource.first())
                        .withEntity(jsonType, serialize(resource.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Given POST maskedResourceOffset to KafkaController - runnable is then called 'late' to simulate arrival of offsets *after* client request
        Runnable postOffsets = () -> maskedResourceOffsetTopic.forEach(offset -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/masked-resource-offset", HOST, port))
                        .withHeaders(offset.first())
                        .withEntity(jsonType, serialize(offset.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());

        // When
        // Send each websocketMessage request and receive responses
        Source<Message, NotUsed> wsMsgSource = Source.fromIterator(websocketRequests::iterator).map(this::writeTextMessage);
        Sink<Message, CompletionStage<List<WebSocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebSocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebSocketMessage>>> request = http.singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%s/resource/" + requestToken, HOST, port)),
                clientFlow,
                akkaMaterializer);
        // Get the (HTTP) response, a websocket upgrade
        request.first().toCompletableFuture()
                .get(1, TimeUnit.SECONDS);

        // Late POST of offsets after client request has been initialized
        postOffsets.run();

        // Get the result of the client sink, a list of (WebSocket) responses
        LinkedList<WebSocketMessage> actualResponses = new LinkedList<>(request.second().toCompletableFuture()
                .get(30 + websocketRequests.size(), TimeUnit.SECONDS));

        // Then
        // Assert each received response matches up with the expected
        assertThat(actualResponses)
                .as("Testing Websocket Response")
                .isEqualTo(websocketResponses);

        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                .withGroupId("error-topic-test-consumer")
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe = Consumer
                .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("error-topic").getName()))
                .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);

        // When you read off the error queue
        LinkedList<ConsumerRecord<String, AuditErrorMessage>> errorResults = LongStream.range(0, auditErrorMessages.size())
                .mapToObj(i -> errorProbe.requestNext(FiniteDuration.create(20 + auditErrorMessages.size(), TimeUnit.SECONDS)))
                .collect(Collectors.toCollection(LinkedList::new));

        // Then
        // The messages on the error topic are as expected
        assertAll("Asserting on the error topic",
                // One error is produced
                () -> assertThat(errorResults)
                        .as("Assert that there is one error on the error topic")
                        .hasSize(1),

                () -> assertThat(errorResults.get(0).value())
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(Throwable.class)
                        .ignoringFields("timestamp")
                        .isEqualTo(auditErrorMessages.get(0))
        );
    }

    // Handle deserialising JSON TextMessages to WebSocketMessages
    private WebSocketMessage readTextMessage(final Message message) {
        // Akka will sometimes convert a StrictMessage to a StreamedMessage, so we have to handle both cases here
        StringBuilder builder;
        if (message.asTextMessage().isStrict()) {
            builder = new StringBuilder(message.asTextMessage().getStrictText());
        } else {
            builder = message.asTextMessage().getStreamedText()
                    .runFold(new StringBuilder(), StringBuilder::append, this.akkaActorSystem)
                    .toCompletableFuture().join();
        }
        return deserialize(builder.toString(), WebSocketMessage.class);
    }

    // Handle serialising WebSocketMessages to JSON TextMessages
    private Message writeTextMessage(final WebSocketMessage message) {
        return new Strict(serialize(message));
    }
}
