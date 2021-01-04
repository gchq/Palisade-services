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

package uk.gov.gchq.palisade.component.filteredresource.web;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.japi.Pair;
import akka.kafka.ConsumerMessage.CommittableOffset;
import akka.kafka.javadsl.Consumer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.component.filteredresource.repository.MapTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.WebsocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.service.WebsocketEventService;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.AuditServiceSinkFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory;
import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.filteredresource.web.router.WebsocketRouter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class AkkaWebsocketTest {
    static final Logger LOGGER = LoggerFactory.getLogger(AkkaWebsocketTest.class);
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final String HOST = "localhost";
    static final int PORT = 18080;
    // The tests (and server) will send N messages (additionally, the server will be given N - 1 resources to return, which will be followed by 1 COMPLETE message)
    static final int N_MESSAGES = 100;

    // Test data
    final String token = "test-token";
    final FileResource testResource = new FileResource()
            .id("/test/file")
            .type("java.util.String")
            .serialisedFormat("text")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-data-service"))
            .parent(new SystemResource().id("/test"));
    final FilteredResourceRequest testRequest = FilteredResourceRequest.Builder.create()
            .withUserId("test-user")
            .withResourceId("/test")
            .withContext(new Context())
            .withResource(testResource);
    final CommittableOffset mockCommittable = Mockito.mock(CommittableOffset.class);

    // Websocket test objects
    final TokenOffsetPersistenceLayer persistenceLayer = new MapTokenOffsetPersistenceLayer();
    final ActorRef<TokenOffsetCommand> offsetController = TokenOffsetController.create(persistenceLayer);
    final FilteredResourceSourceFactory sourceFactory = (token, offset) -> Source.repeat(new Pair<>(testRequest, mockCommittable))
            .take(N_MESSAGES - 1)
            .mapMaterializedValue(notUsed -> Consumer.createNoopControl());
    // This reference is updated by our audit service and wiped clean in the setUp() method before each test
    final AtomicReference<List<FilteredResourceRequest>> auditedResources = new AtomicReference<>(Collections.emptyList());
    final AuditServiceSinkFactory sinkFactory = token -> Sink.foreach(pair -> auditedResources
            .updateAndGet(list -> Stream.of(list, Collections.singletonList(pair.first()))
                    .flatMap(List::stream)
                    .collect(Collectors.toList())));
    // Finally, create the websocketEventService from its parts
    final WebsocketEventService websocketEventService = new WebsocketEventService(offsetController, sinkFactory, sourceFactory);

    // Websocket endpoint to be tested
    final WebsocketRouter wsRouter = new WebsocketRouter(websocketEventService, MAPPER);
    // Akka runtime
    final ActorSystem system = ActorSystem.create("websocket-test");
    final Materializer materializer = Materializer.createMaterializer(system);

    // Server will be torn down and recreated for each test, thus not final
    AkkaHttpServer server;


    @BeforeEach
    void setUp() {
        server = new AkkaHttpServer(HOST, PORT, List.of(wsRouter));
        server.serveForever(system);
        // Reset the audited resources so far
        auditedResources.set(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        server.terminate();
    }

    @Test
    void websocketManyPingTest() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' PING messages and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);
        // Create payload test message
        WebsocketMessage wsMsg = WebsocketMessage.Builder.create().withType(MessageType.PING).noHeaders().noBody();
        Source<Message, NotUsed> wsMsgSource = Source.repeat(wsMsg).take(N_MESSAGES).map(this::writeTextMessage);
        Sink<Message, CompletionStage<List<WebsocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebsocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());

        // **
        // When - the client submits these websocket messages to the server
        // **

        // Make the websocket connection request, sending the payload in the clientFlow
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebsocketMessage>>> request = Http.get(system).singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%d/resource/" + token, HOST, PORT)),
                clientFlow,
                materializer);

        // Get the (HTTP) response, a websocket upgrade
        WebSocketUpgradeResponse wsUpgrade = request.first().toCompletableFuture()
                .get(N_MESSAGES, TimeUnit.SECONDS);
        LOGGER.info("Websocket request got WebsocketUpgrade response: {}", wsUpgrade.response());

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (Websocket) responses
        CompletableFuture<List<WebsocketMessage>> sinkFuture = request.second().toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while collecting list of websocket responses", throwable));

        assertThat(sinkFuture.get(N_MESSAGES, TimeUnit.SECONDS))
                .hasSize(N_MESSAGES)
                // Assert PING -> PONG
                .allSatisfy(message -> assertThat(message)
                        .extracting(WebsocketMessage::getType)
                        .isIn(MessageType.PONG, MessageType.COMPLETE));

        // Nothing should have been audited
        assertThat(auditedResources.get()).hasSize(3);
    }

    @Test
    void websocketReadResourcesTest() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' CTS messages and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);
        // Create payload test message
        WebsocketMessage wsMsg = WebsocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
        Source<Message, NotUsed> wsMsgSource = Source.repeat(wsMsg).take(N_MESSAGES).map(this::writeTextMessage);
        Sink<Message, CompletionStage<List<WebsocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebsocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());

        // **
        // When - the client submits these websocket messages to the server
        // **

        // Make the websocket connection request, sending the payload in the clientFlow
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebsocketMessage>>> request = Http.get(system).singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%d/resource/" + token, HOST, PORT)),
                clientFlow,
                materializer);

        // Get the (HTTP) response, a websocket upgrade
        WebSocketUpgradeResponse wsUpgrade = request.first().toCompletableFuture()
                .get(N_MESSAGES, TimeUnit.SECONDS);
        LOGGER.info("Websocket request got WebsocketUpgrade response: {}", wsUpgrade.response());

        // Get the result of the client sink, a list of (Websocket) responses
        CompletableFuture<List<WebsocketMessage>> sinkFuture = request.second().toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while collecting list of websocket responses", throwable));

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (Websocket) responses
        LinkedList<WebsocketMessage> results = new LinkedList<>(sinkFuture.get(N_MESSAGES, TimeUnit.SECONDS));
        assertThat(results)
                .hasSize(N_MESSAGES);

        // Assert CTS -> COMPLETE for last messages
        assertThat(results.getLast())
                .extracting(WebsocketMessage::getType)
                .isEqualTo(MessageType.COMPLETE);
        results.removeLast();
        assertThat(results)
                .allSatisfy(message -> {
                    // Assert CTS -> RESOURCE for not-last messages
                    assertThat(message)
                            .extracting(WebsocketMessage::getType)
                            .isEqualTo(MessageType.RESOURCE);
                    // Assert the resource returned was the one expected
                    assertThat(message)
                            .extracting(msg -> msg.getBodyObject(FileResource.class))
                            .isEqualTo(testResource);
                });

        // Each request should have been audited
        assertThat(auditedResources.get())
                .isNotEmpty()
                .hasSize(N_MESSAGES - 1) // excluding COMPLETE
                .allSatisfy(auditedFilteredResourceRequest -> assertThat(auditedFilteredResourceRequest)
                        .extracting(FilteredResourceRequest::getResourceNode)
                        .isEqualTo(MAPPER.valueToTree(testResource)));
    }

    @Test
    void websocketInterleavedMessagesTest() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' PING-followed-by-CTS message pairs and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);
        // Create payload test messages
        WebsocketMessage pingMsg = WebsocketMessage.Builder.create().withType(MessageType.PING).noHeaders().noBody();
        WebsocketMessage ctsMsg = WebsocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
        Source<Message, NotUsed> pingMsgSrc = Source.repeat(pingMsg).take(N_MESSAGES).map(this::writeTextMessage);
        Source<Message, NotUsed> ctsMsgSrc = Source.repeat(ctsMsg).take(N_MESSAGES).map(this::writeTextMessage);
        // Interleave PINGs and CTSes
        Source<Message, NotUsed> wsMsgSource = pingMsgSrc.interleave(ctsMsgSrc, 1).delay(Duration.ofMillis(150L), DelayOverflowStrategy.backpressure());
        Sink<Message, CompletionStage<List<WebsocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebsocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());

        // **
        // When - the client submits these websocket messages to the server
        // **

        // Make the websocket connection request, sending the payload in the clientFlow
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebsocketMessage>>> request = Http.get(system).singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%d/resource/" + token, HOST, PORT)),
                clientFlow,
                materializer);

        // Get the (HTTP) response, a websocket upgrade
        WebSocketUpgradeResponse wsUpgrade = request.first().toCompletableFuture()
                .get(N_MESSAGES, TimeUnit.SECONDS);
        LOGGER.info("Websocket request got WebsocketUpgrade response: {}", wsUpgrade.response());

        // Get the result of the client sink, a list of (Websocket) responses
        CompletableFuture<List<WebsocketMessage>> sinkFuture = request.second().toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while collecting list of websocket responses", throwable));

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (Websocket) responses
        List<WebsocketMessage> results = sinkFuture.get(N_MESSAGES, TimeUnit.SECONDS);
        assertThat(results)
                .hasSize(N_MESSAGES * 2);

        // De-interleave the two lists (hopefully the even ones are PINGs and the odd ones RESOURCEs)
        LinkedList<WebsocketMessage> pongMessages = IntStream.range(0, N_MESSAGES * 2)
                .filter(x -> x % 2 == 0)
                .mapToObj(results::get)
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<WebsocketMessage> resourceMessages = IntStream.range(0, N_MESSAGES * 2)
                .filter(x -> x % 2 != 0)
                .mapToObj(results::get)
                .collect(Collectors.toCollection(LinkedList::new));

        assertThat(pongMessages)
                .as("Assert PING -> PONG")
                .hasSize(N_MESSAGES)
                .allSatisfy(message -> assertThat(message)
                        .extracting(WebsocketMessage::getType)
                        .isEqualTo(MessageType.PONG));

        assertThat(resourceMessages.getLast())
                .as("Assert CTS -> COMPLETE for last messages")
                .extracting(WebsocketMessage::getType)
                .isEqualTo(MessageType.COMPLETE);
        resourceMessages.removeLast();
        assertThat(resourceMessages)
                .allSatisfy(message -> {
                    assertThat(message)
                            .as("Assert CTS -> RESOURCE for not-last messages")
                            .extracting(WebsocketMessage::getType)
                            .isEqualTo(MessageType.RESOURCE);
                    assertThat(message)
                            .as("Assert the resource returned was the one expected")
                            .extracting(msg -> msg.getBodyObject(FileResource.class))
                            .isEqualTo(testResource);
                });

        assertThat(auditedResources.get())
                .as("Each request should have been audited")
                .isNotEmpty()
                .hasSize(N_MESSAGES - 1) // excluding COMPLETE
                .allSatisfy(auditedFilteredResourceRequest -> assertThat(auditedFilteredResourceRequest)
                        .extracting(FilteredResourceRequest::getResourceNode)
                        .isEqualTo(MAPPER.valueToTree(testResource)));
    }

    // Handle deserialising JSON TextMessages to WebsocketMessages
    private WebsocketMessage readTextMessage(final Message message) {
        // Akka will sometimes convert a StrictMessage to a StreamedMessage, so we have to handle both cases here
        StringBuilder builder;
        if (message.asTextMessage().isStrict()) {
            builder = new StringBuilder(message.asTextMessage().getStrictText());
        } else {
            builder = message.asTextMessage().getStreamedText()
                    .runFold(new StringBuilder(), StringBuilder::append, this.system)
                    .toCompletableFuture().join();
        }
        try {
            return MAPPER.readValue(builder.toString(), WebsocketMessage.class);
        } catch (IOException e) {
            throw new SerializationFailedException("Failed to read ws message", e);
        }
    }

    // Handle serialising WebsocketMessages to JSON TextMessages
    private Message writeTextMessage(final WebsocketMessage message) {
        try {
            return new Strict(MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write ws message", e);
        }
    }

}
