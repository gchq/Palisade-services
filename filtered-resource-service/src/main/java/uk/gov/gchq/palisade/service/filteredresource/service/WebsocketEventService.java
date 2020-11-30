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

package uk.gov.gchq.palisade.service.filteredresource.service;

import akka.NotUsed;
import akka.http.javadsl.model.ws.Message;
import akka.http.scaladsl.model.ws.BinaryMessage;
import akka.japi.Pair;
import akka.kafka.ConsumerMessage.CommittableOffset;
import akka.kafka.javadsl.Consumer.Control;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.MessageType;
import uk.gov.gchq.palisade.service.filteredresource.model.WebsocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.AuditServiceSinkFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory;
import uk.gov.gchq.palisade.service.filteredresource.stream.util.ConditionalGraph;

import java.util.Map;
import java.util.Optional;

/**
 * When a client connects via websocket, the {@code uk.gov.gchq.palisade.service.filteredresource.web.router.WebsocketRouter}
 * creates an instance of the {@link WebsocketEventService#createFlowGraph(String)} to handle the rest of the request.
 * The service goes through the following steps while returning resources:
 * <ul>
 *   <li> get the topic offset for this token, defaulting to "now"
 * <!--  <li> send any "early" errors to the client (eg. user-service exceptions) -->
 *   <li> send all appropriate masked resources to the client using the pre-calculated commit offset
 * <!--  <li> send any "late" errors to the client (eg. resource-service or policy-service exceptions) -->
 * </ul>
 */
public class WebsocketEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketEventService.class);

    private final AuditServiceSinkFactory auditSinkFactory;
    private final FilteredResourceSourceFactory resourceSourceFactory;

    /**
     * Default constructor for a new WebsocketEventService, supplying the persistence layer for retrieving token offsets.
     * This will continually listen to a client's websocket for RTS/CTS handshakes, sending either errors or resources
     * back to the client as required.
     *
     * @param auditSinkFactory a factory for creating an akka-streams {@link Sink} to the audit "success" queue for a given token
     * @param  resourceSourceFactory a factory for creating an akka-streams {@link Source} from the upstream "masked-resource" queue for a given token
     */
    public WebsocketEventService(
            final AuditServiceSinkFactory auditSinkFactory,
            final FilteredResourceSourceFactory resourceSourceFactory) {
        this.auditSinkFactory = auditSinkFactory;
        this.resourceSourceFactory = resourceSourceFactory;
    }

    /**
     * Create a flow from incoming to outgoing Websocket {@link Message}s.
     * These are expected to be {@link BinaryMessage}s of json-serialised {@link WebsocketMessage}s.
     * <p>
     * This flow will accept the client {@link MessageType}s and return server types as follows:
     * <ul>
     *     <li> {@link MessageType#PING} replies with {@link MessageType#PONG}
     *     <li> {@link MessageType#CTS} replies with one of {@link MessageType#RESOURCE} or {@link MessageType#COMPLETE}
     * </ul>
     * <p>
     * All other incoming types of message will be discarded. No other outgoing types of message will be produced.
     *
     * @param token the token for this flowGraph - this is used when creating the audit {@link Sink} and resource {@link Source}
     * @return a flow from client requests to server responses
     */
    public Flow<WebsocketMessage, WebsocketMessage, NotUsed> createFlowGraph(final String token) {
        return Flow.<WebsocketMessage>create()
                // Log some details of each client request
                .map(wsMsg -> {
                    LOGGER.trace("Received message {} from client", wsMsg);
                    return wsMsg;
                })

                // Register handlers for each MessageType
                // Any messages with MessageTypes other than those listed here are dropped
                .via(ConditionalGraph.map(x -> x.getType().ordinal(), Map.of(
                        MessageType.PING.ordinal(), this.onPing(token),
                        MessageType.CTS.ordinal(), this.onCts(token)
                )));
    }

    /**
     * Handle {@link MessageType#PING} messages, expected to return a {@link MessageType#PONG} message in response.
     * This may handle some additional form of validation in the future.
     *
     * @param token the token for this client
     * @return a flow from SUBSCRIBE client requests to server responses
     */
    private Flow<WebsocketMessage, WebsocketMessage, NotUsed> onPing(final String token) {
        return Flow.<WebsocketMessage>create()
                // Reply to the client's PING request with a PONG (application-layer, not websocket TCP-frame layer)
                .map(message -> WebsocketMessage.Builder.create()
                        .withType(MessageType.PONG)
                        .withHeader("token", token).noHeaders()
                        .noBody()
                );
    }

    /**
     * Handle {@link MessageType#CTS} messages, expected to return a {@link MessageType#RESOURCE} or {@link MessageType#COMPLETE} message in response.
     * This zips the flow of filtered resources from kafka to the flow of {@link MessageType#CTS} messages from the client.
     * This ensures <i>every</i> resource is paired up with <i>every</i> client resource request in a strict one-to-one manner,
     * while still making best use of asynchronous akka streams.
     *
     * @param token the token for this client
     * @return a flow from SUBSCRIBE client requests to server responses
     */
    private Flow<WebsocketMessage, WebsocketMessage, NotUsed> onCts(final String token) {
        // RESOURCE messages are Optional::of, COMPLETE messages are Optional::empty
        // This final message is part of the resourceSource stream completing, not the server's websocket processing flow
        // Otherwise, COMPLETE is returned when the client 'completes' their flow of sent websocket messages (ie. websocket close)
        // At the end of the stream (upon seeing Optional.empty()), we will send a COMPLETE message
        final Source<Optional<Pair<FilteredResourceRequest, CommittableOffset>>, Control> resourceSource =
                this.resourceSourceFactory.create(token)
                        .map(Optional::of)
                        .concat(Source.single(Optional.empty()));

        // Audit client's authorisation for this resource
        final Sink<Optional<Pair<FilteredResourceRequest, CommittableOffset>>, NotUsed> auditSink =
                Flow.<Optional<Pair<FilteredResourceRequest, CommittableOffset>>>create()
                        .flatMapConcat(optionalRequest -> Source.fromJavaStream(optionalRequest::stream))
                        .to(this.auditSinkFactory.create(token));

        return Flow.<WebsocketMessage>create()
                // Connect each CTS message with a processed leafResource
                .zip(resourceSource)
                // Drop the CTS message, we don't care about it's contents beyond the MessageType
                .map(Pair::second)

                // Each filtered resource request is audited as soon as we receive a CTS message for it from the client (before the resource is returned to the client)
                .alsoTo(auditSink)
                // Drop the now-committed offset from the pair, keeping just a filteredResourceRequest
                .via(Flow.<Optional<Pair<FilteredResourceRequest, CommittableOffset>>>create()
                        .map(optionalPair -> optionalPair.map(Pair::first)))

                // Convert the leafResource into a WebsocketMessage response object, copying appropriate headers
                .map(optionalRequest -> optionalRequest
                        // RESOURCE type, copy headers, leafResource body
                        .map(request -> WebsocketMessage.Builder.create()
                                .withType(MessageType.RESOURCE)
                                .noHeaders()
                                .withBody(request.getResourceNode()))
                        // COMPLETE type, copy headers, no body
                        .orElseGet(() -> WebsocketMessage.Builder.create()
                                .withType(MessageType.COMPLETE)
                                .noHeaders()
                                .noBody())
                );
    }
}
