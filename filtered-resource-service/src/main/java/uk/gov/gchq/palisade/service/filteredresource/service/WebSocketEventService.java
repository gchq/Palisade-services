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

package uk.gov.gchq.palisade.service.filteredresource.service;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.http.javadsl.model.ws.Message;
import akka.http.scaladsl.model.ws.BinaryMessage;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.filteredresource.model.AuditableWebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessagePersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.AuditServiceSinkFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.util.ConditionalGraph;

import java.util.Map;
import java.util.Optional;

/**
 * When a client connects via websocket, the {@code uk.gov.gchq.palisade.service.filteredresource.web.router.WebSocketRouter}
 * creates an instance of the {@link WebSocketEventService#createFlowGraph(String)} to handle the rest of the request.
 * The service goes through the following steps while returning resources:
 * <ul>
 *   <li> get the topic offset for this token, defaulting to "now"
 * <!--  <li> send any "early" errors to the client (eg. user-service exceptions) -->
 *   <li> send all appropriate masked resources to the client using the pre-calculated commit offset
 * <!--  <li> send any "late" errors to the client (eg. resource-service or policy-service exceptions) -->
 * </ul>
 */
public class WebSocketEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventService.class);
    private static final Integer PARALLELISM = 1;

    private final ActorRef<TokenOffsetCommand> tokenOffsetController;
    private final AuditServiceSinkFactory auditSinkFactory;
    private final FilteredResourceSourceFactory resourceSourceFactory;
    private final TokenAuditErrorMessagePersistenceLayer tokenAuditErrorMessagePersistenceLayer;

    /**
     * Default constructor for a new WebSocketEventService, supplying the persistence layer for retrieving token offsets.
     * This will continually listen to a client's websocket for RTS/CTSR/CTSE handshakes, sending either errors or resources
     * back to the client as required.
     *
     * @param tokenOffsetController                  the instance of the {@link TokenOffsetController}, which will handle reporting early and late offsets for a given token
     * @param auditSinkFactory                       a factory for creating an akka-streams {@link Sink} to the audit "success" queue for a given token
     * @param resourceSourceFactory                  a factory for creating an akka-streams {@link Source} from the upstream "masked-resource" queue for a given token
     * @param tokenAuditErrorMessagePersistenceLayer access to the persistence layer containing exceptions for a given token
     */
    public WebSocketEventService(
            final ActorRef<TokenOffsetCommand> tokenOffsetController,
            final AuditServiceSinkFactory auditSinkFactory,
            final FilteredResourceSourceFactory resourceSourceFactory,
            final TokenAuditErrorMessagePersistenceLayer tokenAuditErrorMessagePersistenceLayer) {
        this.tokenOffsetController = tokenOffsetController;
        this.auditSinkFactory = auditSinkFactory;
        this.resourceSourceFactory = resourceSourceFactory;
        this.tokenAuditErrorMessagePersistenceLayer = tokenAuditErrorMessagePersistenceLayer;
    }

    /**
     * Create a flow from incoming to outgoing WebSocket {@link Message}s.
     * These are expected to be {@link BinaryMessage}s of json-serialised {@link WebSocketMessage}s.
     * <p>
     * This flow will accept the client {@link MessageType}s and return server types as follows:
     * <ul>
     *     <li> {@link MessageType#PING} replies with {@link MessageType#PONG}
     *     <li> {@link MessageType#CTSR} replies with one of {@link MessageType#RESOURCE} or {@link MessageType#COMPLETE}
     *     <li> {@link MessageType#CTSE} replies with one of {@link MessageType#ERROR} or {@link MessageType#NO_ERROR}
     * </ul>
     * <p>
     * All other incoming types of message will be discarded. No other outgoing types of message will be produced.
     *
     * @param token the token for this flowGraph - this is used when creating the audit {@link Sink} and resource {@link Source}
     * @return a flow from client requests to server responses
     */
    public Flow<WebSocketMessage, WebSocketMessage, NotUsed> createFlowGraph(final String token) {
        return Flow.<WebSocketMessage>create()
                // Log some details of each client request
                .map((WebSocketMessage wsMsg) -> {
                    LOGGER.trace("Received message {} from client", wsMsg);
                    return wsMsg;
                })

                // Register handlers for each MessageType
                // Any messages with MessageTypes other than those listed here are dropped
                .via(ConditionalGraph.map(x -> x.getType().ordinal(), Map.of(
                        // On PING message, return PONG
                        MessageType.PING.ordinal(), onPing(token),

                        // On CTSR message, get the offset for the token from persistence, then return results
                        // Usually returns RESOURCE ....*n, RESOURCE, COMPLETE, if redis is dead then return ERROR, and then NO_ERROR
                        MessageType.CTSR.ordinal(), onCtsResource(token),

                        // On CTSE messages, get the exceptions for the token from the persistence, and return the results
                        // returns ERROR, ....*n, ERROR and then NO_ERROR
                        MessageType.CTSE.ordinal(), onCtsError(token)
                )));
    }

    /**
     * Handle error messages, will send a {@link MessageType#ERROR} for each error associated with the token,
     * and then a {@link MessageType#NO_ERROR} message to signify that all errors have been processed and returned
     *
     * @param token the unique request token
     * @return An Exception for the token followed by a {@link MessageType#NO_ERROR} when all exceptions have been processed
     */
    private Flow<WebSocketMessage, WebSocketMessage, NotUsed> onCtsError(final String token) {
        return Flow.<WebSocketMessage>create()
                // Get the errors for the token from the repository
                .mapAsync(PARALLELISM, (WebSocketMessage message) -> tokenAuditErrorMessagePersistenceLayer.popAuditErrorMessage(token))

                // Build the WebSocketMessage with the {@link MessageType#ERROR} and AuditErrorMessage in the body
                .map(message -> message.map(auditErrorMessage -> WebSocketMessage.Builder.create()
                        .withType(MessageType.ERROR)
                        .withHeader(Token.HEADER, token).noHeaders()
                        .withBody(auditErrorMessage)))

                .takeWhile(Optional::isPresent)
                .map(Optional::get)

                // Then send a NO_ERROR to signify that all errors for this token have been sent
                .concat(Source.single(WebSocketMessage.Builder.create()
                        .withType(MessageType.NO_ERROR)
                        .withHeader(Token.HEADER, token).noHeaders()
                        .noBody()));
    }

    /**
     * Handle {@link MessageType#PING} messages, expected to return a {@link MessageType#PONG} message in response.
     * This may handle some additional form of validation in the future.
     *
     * @param token the token for this client
     * @return a flow from {@link MessageType#PING} client requests to {@link MessageType#PONG} server responses
     */
    private static Flow<WebSocketMessage, WebSocketMessage, NotUsed> onPing(final String token) {
        return Flow.<WebSocketMessage>create()
                // Reply to the client's PING request with a PONG (application-layer, not websocket TCP-frame layer)
                .map(message -> WebSocketMessage.Builder.create()
                        .withType(MessageType.PONG)
                        .withHeader(Token.HEADER, token).noHeaders()
                        .noBody()
                );
    }

    /**
     * Handle {@link MessageType#CTSR} messages, expected to return a {@link MessageType#RESOURCE} or {@link MessageType#COMPLETE}
     * message in response.
     * This zips the flow of filtered resources from kafka to the flow of {@link MessageType#CTSR} messages from the client.
     * This ensures <i>every</i> resource is paired up with <i>every</i> client CTS in a strict one-to-one manner, while still
     * making best use of asynchronous akka streams.
     * If an error occurs getting the offset for a token from persistence (eg redis is down), a {@link MessageType#ERROR} will be
     * returned, followed by a {@link MessageType#COMPLETE}.
     *
     * @param token the token for this client
     * @return a flow from {@link MessageType#CTSR} client requests to {@link MessageType#RESOURCE}, {@link MessageType#ERROR} or
     * {@link MessageType#COMPLETE} server responses
     */
    private Flow<WebSocketMessage, WebSocketMessage, NotUsed> onCtsResource(final String token) {
        return Flow.<WebSocketMessage>create()
                // Connect each CTSR message with a processed leafResource or error
                .zip(this.createResourceSource(token))

                // Drop the CTSR message, we don't care about it's contents beyond the MessageType
                .map(Pair::second)

                // Connect to the audit topic kafka stream for resources returned to the client
                // Each filtered resource request is audited as soon as we receive a CTSR message
                // for it from the client (before the resource is returned to the client)
                .alsoTo(Flow.<AuditableWebSocketMessage>create()
                        .map(AuditableWebSocketMessage::getAuditSuccessPair)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .to(this.auditSinkFactory.create(token)))

                .map(AuditableWebSocketMessage::getWebSocketMessage);
    }

    /**
     * A finite stream of {@link WebSocketMessage}s, representing either {@link MessageType#RESOURCE}s
     * or {@link MessageType#ERROR}s.
     *
     * @param token the token for this client
     * @return a source of {@link WebSocketMessage}s paired with an auditable context
     */
    private Source<AuditableWebSocketMessage, NotUsed> createResourceSource(final String token) {
        // Connect to the client's stream of resources by retrieving the token's offset and connecting to kafka
        return Source.single(token)
                // Get the offset for the given token
                .via(TokenOffsetController.asGetterFlow(this.tokenOffsetController))

                // Differing behaviour depending on whether the offset was found:
                // * many RESOURCE messages if an offset was found
                // * single ERROR message if an exception was thrown
                .flatMapConcat(offsetResponse -> Optional.ofNullable(offsetResponse.getOffset())
                        // If an offset was successfully found for this token, emit many RESOURCE messages
                        .map(offset -> this.resourceSourceFactory.create(token, offset)
                                // Convert to an internal datatype capturing all necessary information
                                .map(pair -> AuditableWebSocketMessage.Builder.create()
                                        .withWebSocketMessage(WebSocketMessage.Builder.create()
                                                .withType(MessageType.RESOURCE)
                                                .withHeader(Token.HEADER, token).noHeaders()
                                                .withBody(pair.first().getResourceNode()))
                                        .withAuditablePair(pair))

                                // Ignore this stream's materialization
                                .mapMaterializedValue(ignoredMat -> NotUsed.notUsed()))

                        // If an error occurred finding the offset, emit a single ERROR message
                        .orElse(Source.single(AuditableWebSocketMessage.Builder.create()
                                .withWebSocketMessage(WebSocketMessage.Builder.create()
                                        .withType(MessageType.ERROR)
                                        .withHeader(Token.HEADER, token).noHeaders()
                                        .withBody(offsetResponse.getException()))
                                .withoutAuditable())

                                // Then send a NO_ERROR to signify that all errors for this token have been sent
                                .concat(Source.single(AuditableWebSocketMessage.Builder.create()
                                        .withWebSocketMessage(WebSocketMessage.Builder.create()
                                                .withType(MessageType.NO_ERROR)
                                                .withHeader(Token.HEADER, token).noHeaders()
                                                .noBody())
                                        .withoutAuditable()))))

                // Append COMPLETE message
                .concat(Source.single(AuditableWebSocketMessage.Builder.create()
                        .withWebSocketMessage(WebSocketMessage.Builder.create()
                                .withType(MessageType.COMPLETE)
                                .withHeader(Token.HEADER, token).noHeaders()
                                .noBody())
                        .withoutAuditable()));
    }

}
