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

package uk.gov.gchq.palisade.contract.filteredresource.kafka;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.japi.Pair;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.util.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
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

import uk.gov.gchq.palisade.contract.filteredresource.UserServiceAuditErrorMessage;
import uk.gov.gchq.palisade.contract.filteredresource.kafka.KafkaInitializer.ErrorDeserializer;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.common.Token;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.filteredresource.common.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
    /*
     * We cannot inspect the errors generated by the service by any means other than reading straight from the kafka error topic.
     * Construct a single error probe for the lifetime of this test class and use that.
     * Due to commit offsets, a failed assertion on the error topic in a test may further fail all further error-topic assertions in future parameterised tests.
     */
    private static Probe<ConsumerRecord<String, AuditErrorMessage>> errorProbe;
    @Value("${server.port}")
    private Integer port;
    @Autowired
    private TokenOffsetPersistenceLayer persistenceLayer;
    @Autowired
    private TokenErrorMessagePersistenceLayer errorMessagePersistenceLayer;
    @Autowired
    private ActorSystem akkaActorSystem;
    @Autowired
    private Materializer akkaMaterializer;
    @Autowired
    private ConsumerTopicConfiguration consumerTopicConfiguration;

    @NotNull
    private static TriFunction<String, String, String, WebSocketMessage> getErrorBuilder() {
        return (token, serviceName, errorMessage) -> WebSocketMessage.Builder.create()
                .withType(MessageType.ERROR)
                .withHeader(Token.HEADER, token).withHeader("service-name", serviceName)
                .noHeaders()
                .withBody(errorMessage);
    }

    @NotNull
    private static BiFunction<String, LeafResource, WebSocketMessage> getResponseBuilder() {
        return (token, leafResource) -> WebSocketMessage.Builder.create()
                .withType(MessageType.RESOURCE)
                .withHeader(Token.HEADER, token)
                .noHeaders()
                .withBody(leafResource);
    }

    @NotNull
    private static Function<String, FilteredResourceRequest> getResourceBuilder() {
        return resourceId -> FilteredResourceRequest.Builder.create()
                .withUserId("userId")
                .withResourceId("file:/file/")
                .withContext(new Context().purpose("purpose"))
                .withResource(new FileResource()
                        .id("file:/file/" + resourceId)
                        .serialisedFormat("fmt")
                        .type("type")
                        .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))
                        .parent(new SystemResource().id("file:/file/")));
    }

    @NotNull
    private static Function<String, WebSocketMessage> getCompleteMsgBuilder() {
        return (token) -> WebSocketMessage.Builder.create()
                .withType(MessageType.COMPLETE)
                .withHeader(Token.HEADER, token)
                .noHeaders()
                .noBody();
    }

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

    @BeforeEach
    void setUpClass() {
        // Only create the error probe once, but a static context can't use the autowired members
        // So if null, this is the first
        if (errorProbe == null) {
            ConsumerSettings<String, AuditErrorMessage> consumerSettings = ConsumerSettings
                    .create(akkaActorSystem, new StringDeserializer(), new ErrorDeserializer())
                    .withBootstrapServers(KafkaInitializer.KAFKA_CONTAINER.getBootstrapServers())
                    .withGroupId("error-topic-test-consumer");
            errorProbe = Consumer
                    .atMostOnceSource(consumerSettings, Subscriptions.topics(consumerTopicConfiguration.getTopics().get("error-topic").getName()))
                    .runWith(TestSink.probe(akkaActorSystem), akkaMaterializer);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ParameterizedArguments.class)
    void testAkkaRunnableGraph(
            // Parameterised input
            final String requestToken,
            final List<Pair<Iterable<HttpHeader>, FilteredResourceRequest>> maskedResourceTopic,
            final List<Pair<Iterable<HttpHeader>, TopicOffsetMessage>> maskedResourceOffsetTopic,
            final List<Pair<Iterable<HttpHeader>, AuditErrorMessage>> errorTopic,
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
        // POST error to KafkaController
        errorTopic.forEach(error -> http
                .singleRequest(HttpRequest.POST(String.format("http://%s:%d/api/error", HOST, port))
                        .withHeaders(error.first())
                        .withEntity(jsonType, serialize(error.second()).getBytes()))
                .toCompletableFuture()
                .thenAccept(response -> assertThat(response.status().intValue()).isEqualTo(202))
                .join());
        // Write offsets to persistence
        offsetsPersistence.forEach((token, offset) -> persistenceLayer
                .overwriteOffset(token, offset)
                .join());

        CompletableFuture.runAsync(() -> {
            while (errorMessagePersistenceLayer.getAllErrorMessages(requestToken).join().size() != errorTopic.size()) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).get(5, TimeUnit.SECONDS);

        // When
        // Send each websocketMessage request and receive responses
        Source<Message, NotUsed> wsMsgSource = Source.fromIterator(websocketRequests::iterator)
                .map(this::writeTextMessage)
                .delay(Duration.ofMillis(500), DelayOverflowStrategy.backpressure());
        Sink<Message, CompletionStage<List<WebSocketMessage>>> listSink = Flow.<Message>create()
                .map(this::readTextMessage)
                .toMat(Sink.seq(), Keep.right());
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
            // When you read off the error queue
            // Read both those posted and those produced
            LinkedList<ConsumerRecord<String, AuditErrorMessage>> errorResults = LongStream.range(0, errorTopic.size() + auditErrorMessages.size())
                    .mapToObj(i -> errorProbe.requestNext(FiniteDuration.create(20 + auditErrorMessages.size(), TimeUnit.SECONDS)))
                    .collect(Collectors.toCollection(LinkedList::new));
            // Drop the messages posted in the setup for the test
            errorTopic.forEach(postedError -> errorResults.removeFirst());

            // Then
            // The messages on the error topic are as expected
            assertAll("Asserting on the error topic",
                    () -> assertThat(errorResults)
                            .as("Assert that there is one error on the error topic")
                            .hasSize(auditErrorMessages.size()),

                    () -> assertThat(errorResults)
                            .extracting(ConsumerRecord::value)
                            .as("Assert that after ignoring the Throwable object, and differences in timestamp, the AuditErrorMessages are the same")
                            .usingRecursiveComparison()
                            .ignoringAllOverriddenEquals()
                            .ignoringFields("timestamp", "error")
                            .isEqualTo(auditErrorMessages),

                    () -> assertThat(errorResults)
                            .extracting(ConsumerRecord::value)
                            .extracting(AuditErrorMessage::getError)
                            .extracting(Throwable::getMessage)
                            .as("Assert that the error message inside the AuditErrorMessage is the same")
                            .isEqualTo(auditErrorMessages.stream()
                                    .map(AuditErrorMessage::getError)
                                    .map(Throwable::getMessage)
                                    .collect(Collectors.toList()))
            );
        }
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

    /**
     * Construct a stream of test parameters as follows:
     * <ul>
     *     <li> Test for an early client and late query result - the stream is created as the topic-offset message arrives
     *     <li> Test for a late client and early query result - the stream is created using an existing offset in persistence
     *     <li> Test for a missing START stream marker - the query is corrupt, no resources are returned (even if specified), and the error is audited
     *     <li> Test for no resources returned between START and END messages - the request was suspicious and has been audited, no resources to return
     * </ul>
     */
    public static class ParameterizedArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) throws Exception {
            // Builders
            Function<String, FilteredResourceRequest> resourceBuilder = getResourceBuilder();
            Function<Long, TopicOffsetMessage> offsetBuilder = TopicOffsetMessage.Builder.create()::withCommitOffset;
            BiFunction<String, LeafResource, WebSocketMessage> responseBuilder = getResponseBuilder();
            TriFunction<String, String, String, WebSocketMessage> errorBuilder = getErrorBuilder();
            // Special instances
            HttpHeader startHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.START));
            HttpHeader endHeader = RawHeader.create(StreamMarker.HEADER, String.valueOf(StreamMarker.END));
            WebSocketMessage ctsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
            Function<String, WebSocketMessage> completeMsgBuilder = getCompleteMsgBuilder();
            return Stream.of(
                    // Test for 'early' client - topic offset message has offset
                    // Expect to receive the three resources and no errors
                    Arguments.of(
                            "test-token-1",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1"), endHeader), null)
                            ),
                            List.of(Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-1")), offsetBuilder.apply(0L))),
                            List.of(),
                            Map.of(),
                            List.of(ctsMsg, ctsMsg, ctsMsg, ctsMsg),
                            List.of(
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-1", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-1")
                            ),
                            List.of()
                    ),
                    // Test for 'late' client - persistence has offset
                    // Expect to receive the three resources and no errors
                    Arguments.of(
                            "test-token-2",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-2"), endHeader), null)
                            ),
                            List.of(),
                            List.of(),
                            Map.of(
                                    "test-token-2", 0L
                            ),
                            List.of(ctsMsg, ctsMsg, ctsMsg, ctsMsg),
                            List.of(
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-2", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-2")
                            ),
                            List.of()
                    ),
                    // Test no start of stream marker - query is corrupted
                    // Expect to receive no resources, and expect one error on the audit error topic
                    Arguments.of(
                            "test-token-3",
                            List.of(
                                    //No Start Marker
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3"), endHeader), null)
                            ),
                            List.of(Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-3")), offsetBuilder.apply(0L))),
                            List.of(),
                            Map.of(),
                            List.of(ctsMsg),
                            List.of(
                                    errorBuilder.apply("test-token-3", "filtered-resource-service",
                                            "uk.gov.gchq.palisade.service.filteredresource.exception.NoStartMarkerObservedException: No Start Marker was observed for token: " + "test-token-3")
                            ),
                            List.of(
                                    AuditErrorMessage.Builder.create().withUserId("userId")
                                            .withResourceId("file:/file/")
                                            .withContext(new Context().purpose("purpose"))
                                            .withAttributes(Collections.emptyMap())
                                            .withError(new Throwable("No Start Marker was observed for token: " + "test-token-3"))
                            )
                    ),
                    // Test no resources - query was suspicious
                    // Expect to receive no resources, and expect one error on the audit error topic
                    Arguments.of(
                            "test-token-4",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4"), endHeader), null)
                            ),
                            List.of(Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-4")), offsetBuilder.apply(0L))),
                            List.of(),
                            Map.of(),
                            List.of(ctsMsg),
                            List.of(
                                    // No Error is expected to be returned to the client
                                    WebSocketMessage.Builder.create().withType(MessageType.COMPLETE).withHeader(Token.HEADER, "test-token-4").noHeaders().noBody()
                            ),
                            List.of(
                                    AuditErrorMessage.Builder.create().withUserId("unknown")
                                            .withResourceId("unknown")
                                            .withContext(new Context().purpose("unknown"))
                                            .withAttributes(Collections.emptyMap())
                                            .withError(new Throwable("No Resources were observed for token: " + "test-token-4"))
                            )
                    ),
                    Arguments.of(
                            "test-token-5",
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5"), startHeader), null),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5")), resourceBuilder.apply("resource.1")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5")), resourceBuilder.apply("resource.2")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5")), resourceBuilder.apply("resource.3")),
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5"), endHeader), null)
                            ),
                            List.of(Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5")), offsetBuilder.apply(0L))),
                            List.of(
                                    Pair.create(List.of(RawHeader.create(Token.HEADER, "test-token-5")),
                                            UserServiceAuditErrorMessage.Builder.create().withUserId("userId")
                                                    .withResourceId("file:/file/resource.4")
                                                    .withContext(new Context().purpose("purpose"))
                                                    .withAttributes(Collections.emptyMap())
                                                    .withError(new Throwable("No userId matching: " + "userId")))
                            ),
                            Map.of(),
                            List.of(ctsMsg, ctsMsg, ctsMsg, ctsMsg, ctsMsg),
                            List.of(
                                    errorBuilder.apply("test-token-5", "user-service", "No userId matching: userId"),
                                    responseBuilder.apply("test-token-5", resourceBuilder.apply("resource.1").getResource()),
                                    responseBuilder.apply("test-token-5", resourceBuilder.apply("resource.2").getResource()),
                                    responseBuilder.apply("test-token-5", resourceBuilder.apply("resource.3").getResource()),
                                    completeMsgBuilder.apply("test-token-5")
                            ),
                            List.of(/* Empty as this test doesnt output anything on to the error topic */)
                    )
            );
        }
    }
}
