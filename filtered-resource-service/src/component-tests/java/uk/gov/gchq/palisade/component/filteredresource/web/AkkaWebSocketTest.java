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

package uk.gov.gchq.palisade.component.filteredresource.web;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.japi.Pair;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.javadsl.Consumer;
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

import uk.gov.gchq.palisade.component.filteredresource.repository.MapTokenErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.component.filteredresource.repository.MapTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditableWebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageController;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageController.TokenErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.service.WebSocketEventService;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.AuditServiceSinkFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory;
import uk.gov.gchq.palisade.service.filteredresource.web.AkkaHttpServer;
import uk.gov.gchq.palisade.service.filteredresource.web.router.WebSocketRouter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class AkkaWebSocketTest {
    static final Logger LOGGER = LoggerFactory.getLogger(AkkaWebSocketTest.class);
    static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();
    static final String HOST = "localhost";
    static final int PORT = 18080;
    // The tests (and server) will send N messages (additionally, the server will be given N - 1 resources to return, which will be followed by 1 COMPLETE message)
    static int nMESSAGES = 100;

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
            .withContext(new Context().purpose("test-purpose"))
            .withResource(testResource);
    final Committable mockCommittable = Mockito.mock(Committable.class);

    // WebSocket test objects
    final TokenOffsetPersistenceLayer persistenceLayer = new MapTokenOffsetPersistenceLayer();
    final TokenErrorMessagePersistenceLayer errorPersistenceLayer = new MapTokenErrorMessagePersistenceLayer();
    final ActorRef<TokenOffsetCommand> offsetController = TokenOffsetController.create(persistenceLayer);
    final ActorRef<TokenErrorMessageCommand> errorMessageController = TokenErrorMessageController.create(errorPersistenceLayer);
    final FilteredResourceSourceFactory sourceFactory = (token, offset) -> Source.repeat(new Pair<>(testRequest, mockCommittable))
            .take(nMESSAGES - 1)
            .mapMaterializedValue(notUsed -> Consumer.createNoopControl());

    // This reference is updated by our audit service and wiped clean in the setUp() method before each test
    final AtomicReference<LinkedList<FilteredResourceRequest>> auditedResources = new AtomicReference<>(new LinkedList<>());
    final Sink<AuditableWebSocketMessage, CompletionStage<Done>> listSink = Sink.foreach(pair -> auditedResources
            .updateAndGet(list -> {
                list.addLast(pair.getFilteredResourceRequest());
                return list;
            }));
    final AuditServiceSinkFactory sinkFactory = token -> Flow.<AuditableWebSocketMessage>create()
            .filter(message -> message.getCommittable() != null) // Similar to the service implementation, only 'audit' things that are committable
            .toMat(listSink, Keep.right());
    // Finally, create the websocketEventService from its parts
    final WebSocketEventService websocketEventService = new WebSocketEventService(offsetController, errorMessageController, sinkFactory, sourceFactory);

    // WebSocket endpoint to be tested
    final WebSocketRouter wsRouter = new WebSocketRouter(websocketEventService, MAPPER);
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
        auditedResources.set(new LinkedList<>());
    }

    @AfterEach
    void tearDown() {
        server.terminate();
    }

    @Test
    void testWebSocketManyPings() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' PING messages and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);

        // Create the payload data
        WebSocketMessage wsMsg = WebSocketMessage.Builder.create().withType(MessageType.PING).noHeaders().noBody();
        Source<Message, NotUsed> wsMsgSource = Source.repeat(wsMsg).take(nMESSAGES).map(this::writeTextMessage);
        CompletableFuture<List<WebSocketMessage>> sinkFuture = sendAndRecieveMessages(wsMsgSource);

        assertThat(sinkFuture.get(nMESSAGES, TimeUnit.SECONDS))
                .as("Check that the number of response messages matches the number of requests")
                .hasSize(nMESSAGES)
                // Assert PING -> PONG
                .allSatisfy(message -> assertThat(message)
                        .as("All PING messages should be replied to with a PONG")
                        .extracting(WebSocketMessage::getType)
                        .isEqualTo(MessageType.PONG));

        assertThat(auditedResources.get())
                .as("Nothing should have been audited")
                .isEmpty();
    }

    @Test
    void testWebSocketCTSReadResources() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' CTS messages and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);

        // Create the payload data
        WebSocketMessage wsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
        Source<Message, NotUsed> wsMsgSource = Source.repeat(wsMsg).take(nMESSAGES).map(this::writeTextMessage);
        CompletableFuture<List<WebSocketMessage>> sinkFuture = sendAndRecieveMessages(wsMsgSource);

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (WebSocket) responses
        LinkedList<WebSocketMessage> results = new LinkedList<>(sinkFuture.get(nMESSAGES, TimeUnit.SECONDS));
        assertThat(results)
                .as("Check that the number of response messages matches the number of requests")
                .hasSize(nMESSAGES);

        // Assert CTS -> COMPLETE for last messages
        assertThat(results.getLast())
                .as("Assert that the last message is COMPLETE")
                .extracting(WebSocketMessage::getType)
                .isEqualTo(MessageType.COMPLETE);
        results.removeLast();

        assertThat(results)
                .allSatisfy(message -> {
                    // Assert CTS -> RESOURCE for not-last messages
                    assertThat(message)
                            .as("All other responses to client CTS should be RESOURCE")
                            .extracting(WebSocketMessage::getType)
                            .isEqualTo(MessageType.RESOURCE);
                    // Assert the resource returned was the one expected
                    assertThat(message)
                            .as("The RESOURCE returned should be the one expected")
                            .extracting(msg -> msg.getBodyObject(FileResource.class))
                            .isEqualTo(testResource);
                });

        // Each request should have been audited
        assertThat(auditedResources.get())
                .as("Check that the number of audit success messages matches the number of resources returned")
                .hasSize(nMESSAGES - 1) // excluding COMPLETE
                .allSatisfy(auditedFilteredResourceRequest -> assertThat(auditedFilteredResourceRequest)
                        .extracting(FilteredResourceRequest::getResourceNode)
                        .isEqualTo(MAPPER.valueToTree(testResource)));
    }


    @Test
    void testWebSocketInterleavedCTSAndPings() throws InterruptedException, ExecutionException, TimeoutException {
        // **
        // Given - the client will send 'n' PING-followed-by-CTS message pairs and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);
        // Create payload test messages
        WebSocketMessage pingMsg = WebSocketMessage.Builder.create().withType(MessageType.PING).noHeaders().noBody();
        WebSocketMessage ctsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
        Source<Message, NotUsed> pingMsgSrc = Source.repeat(pingMsg).take(nMESSAGES).map(this::writeTextMessage);
        Source<Message, NotUsed> ctsMsgSrc = Source.repeat(ctsMsg).take(nMESSAGES).map(this::writeTextMessage);
        // Interleave PINGs and CTSes
        Source<Message, NotUsed> wsMsgSource = pingMsgSrc.interleave(ctsMsgSrc, 1);
        CompletableFuture<List<WebSocketMessage>> sinkFuture = sendAndRecieveMessages(wsMsgSource);

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (WebSocket) responses
        List<WebSocketMessage> results = sinkFuture.get(nMESSAGES, TimeUnit.SECONDS);
        assertThat(results)
                .as("Check that the number of response messages matches the number of requests")
                .hasSize(nMESSAGES * 2);

        // De-interleave the two lists
        // There is no guarantee that messages of different types are strictly ordered compared to one another, but messages of the same type are
        // e.g. we might see CTS 1, PING 1, CTS 2, PING 2, CTS 3, PING 3 -> | SERVER | -> PONG 1, PONG 2, RESOURCE 1, PONG 3, RESOURCE 2, RESOURCE 3
        LinkedList<WebSocketMessage> pongMessages = results.stream()
                .filter(result -> result.getType().equals(MessageType.PONG))
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<WebSocketMessage> resourceMessages = results.stream()
                .filter(result -> result.getType().equals(MessageType.RESOURCE) || result.getType().equals(MessageType.COMPLETE))
                .collect(Collectors.toCollection(LinkedList::new));

        assertThat(pongMessages)
                .as("All PINGs should be responded to with PONGs")
                .hasSize(nMESSAGES)
                .allSatisfy(message -> assertThat(message)
                        .extracting(WebSocketMessage::getType)
                        .isEqualTo(MessageType.PONG));

        assertThat(resourceMessages.getLast())
                .as("Last response to a CTS should be COMPLETE")
                .extracting(WebSocketMessage::getType)
                .isEqualTo(MessageType.COMPLETE);
        resourceMessages.removeLast();
        assertThat(resourceMessages)
                .allSatisfy(message -> {
                    assertThat(message)
                            .as("All other responses to client CTS should be RESOURCE")
                            .extracting(WebSocketMessage::getType)
                            .isEqualTo(MessageType.RESOURCE);
                    assertThat(message)
                            .as("The RESOURCE returned should be the one expected")
                            .extracting(msg -> msg.getBodyObject(FileResource.class))
                            .isEqualTo(testResource);
                });

        assertThat(auditedResources.get())
                .as("Each request should have been audited")
                .hasSize(nMESSAGES - 1) // excluding COMPLETE
                .as("Check that %s messages are returned, and the RESOURCE returned should be the one expected", nMESSAGES - 1)
                .allSatisfy(auditedFilteredResourceRequest -> assertThat(auditedFilteredResourceRequest)
                        .extracting(FilteredResourceRequest::getResourceNode)
                        .isEqualTo(MAPPER.valueToTree(testResource)));
    }

    @Test
    void testWebSocketReceivesEarlyErrors() throws InterruptedException, ExecutionException, TimeoutException {
        // The tests (and server) will send N messages (additionally, the server will be given N - 1 resources to return, start with 1 ERROR message and end with 1 COMPLETE message)
        int nMessages = 101; // including ERROR
        errorPersistenceLayer.putErrorMessage(token,
                "user-service",
                new Throwable("No userId matching: test-user-1")).join();

        var expectedErrorWebSocketMessage = WebSocketMessage.Builder.create()
                .withType(MessageType.ERROR)
                .withHeader("x-request-token", "test-token").withHeader("service-name", "user-service").noHeaders()
                .withBody("No userId matching: test-user-1");
        // **
        // Given - the client will send 'n' CTS messages and collect the responses to a list
        // **

        // Add a dummy offset to persistence (it is ignored by the mock ResourceSourceFactory function)
        persistenceLayer.overwriteOffset(token, 1L);
        // Create the payload data
        WebSocketMessage wsMsg = WebSocketMessage.Builder.create().withType(MessageType.CTS).noHeaders().noBody();
        Source<Message, NotUsed> wsMsgSource = Source.repeat(wsMsg).take(nMessages).map(this::writeTextMessage);
        CompletableFuture<List<WebSocketMessage>> sinkFuture = sendAndRecieveMessages(wsMsgSource);

        // **
        // Then - check all returned server responses are as expected
        // **

        // Get the result of the client sink, a list of (WebSocket) responses
        LinkedList<WebSocketMessage> results = new LinkedList<>(sinkFuture.get(nMessages, TimeUnit.SECONDS));
        assertThat(results)
                .hasSize(nMessages);

        assertThat(results.getFirst())
                .as("Assert that the first message is an ERROR")
                .usingRecursiveComparison()
                .isEqualTo(expectedErrorWebSocketMessage);
        results.removeFirst();

        // Assert CTS -> COMPLETE for last messages
        assertThat(results.getLast())
                .as("Assert that the last message is COMPLETE")
                .extracting(WebSocketMessage::getType)
                .isEqualTo(MessageType.COMPLETE);
        results.removeLast();

        assertThat(results)
                .allSatisfy(message -> {
                    // Assert CTS -> RESOURCE for not-last messages
                    assertThat(message)
                            .as("All other responses to client CTS should be RESOURCE")
                            .extracting(WebSocketMessage::getType)
                            .isEqualTo(MessageType.RESOURCE);
                    // Assert the resource returned was the one expected
                    assertThat(message)
                            .as("The RESOURCE returned should be the one expected")
                            .extracting(msg -> msg.getBodyObject(FileResource.class))
                            .isEqualTo(testResource);
                });

        // Each request should have been audited
        assertThat(auditedResources.get())
                .as("Check that the number of response messages matches the number of requests")
                .isNotEmpty()
                .hasSize(nMessages - 2) // excluding COMPLETE and ERROR
                .allSatisfy(auditedFilteredResourceRequest -> assertThat(auditedFilteredResourceRequest)
                        .extracting(FilteredResourceRequest::getResourceNode)
                        .isEqualTo(MAPPER.valueToTree(testResource)));
    }

    /**
     * The main test code used to send requests and retrieve messages from the service via a WebSocket
     *
     * @param wsMsgSource A source containing the number of webSocketMessages and the type of messages to send
     * @return a CompletableFuture of a List of messages that have been returned from the service
     * @throws ExecutionException   – if this future completed exceptionally
     * @throws InterruptedException – if the current thread was interrupted while waiting
     * @throws TimeoutException     – if the wait timed out
     */
    private CompletableFuture<List<WebSocketMessage>> sendAndRecieveMessages(final Source<Message, NotUsed> wsMsgSource) throws InterruptedException, ExecutionException, TimeoutException {
        Sink<Message, CompletionStage<List<WebSocketMessage>>> listSink = Flow.<Message>create().map(this::readTextMessage).toMat(Sink.seq(), Keep.right());
        // Create client Sink/Source Flow (send the payload, collect the responses)
        Flow<Message, Message, CompletionStage<List<WebSocketMessage>>> clientFlow = Flow.fromSinkAndSourceMat(listSink, wsMsgSource, Keep.left());

        // **
        // When - the client submits these websocket messages to the server
        // **

        // Make the websocket connection request, sending the payload in the clientFlow
        Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<List<WebSocketMessage>>> request = Http.get(system).singleWebSocketRequest(
                WebSocketRequest.create(String.format("ws://%s:%d/resource/" + token, HOST, PORT)),
                clientFlow,
                materializer);

        // Get the (HTTP) response, a websocket upgrade
        WebSocketUpgradeResponse wsUpgrade = request.first().toCompletableFuture()
                .get(1, TimeUnit.SECONDS);
        LOGGER.info("WebSocket request got WebSocketUpgrade response: {}", wsUpgrade.response());

        // Get the result of the client sink, a list of (WebSocket) responses
        return request.second().toCompletableFuture()
                .exceptionally(throwable -> fail("CompletableFuture failed while collecting list of websocket responses", throwable));
    }

    // Handle deserialising JSON TextMessages to WebSocketMessages
    private WebSocketMessage readTextMessage(final Message message) {
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
            return MAPPER.readValue(builder.toString(), WebSocketMessage.class);
        } catch (IOException e) {
            throw new SerializationFailedException("Failed to read ws message", e);
        }
    }

    // Handle serialising WebSocketMessages to JSON TextMessages
    private Message writeTextMessage(final WebSocketMessage message) {
        try {
            return new Strict(MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to write ws message", e);
        }
    }

}
