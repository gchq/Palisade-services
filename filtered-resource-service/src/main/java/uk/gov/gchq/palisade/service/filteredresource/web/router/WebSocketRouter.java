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

package uk.gov.gchq.palisade.service.filteredresource.web.router;

import akka.NotUsed;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.model.ws.TextMessage.Strict;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.filteredresource.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.WebSocketEventService;

/**
 * A class to route web socket messages
 */
public class WebSocketRouter implements RouteSupplier {
    private final WebSocketEventService webSocketEventService;
    private final ObjectMapper mapper;

    /**
     * Public constructor
     *
     * @param webSocketEventService the {@code WebSocketEventService} value
     * @param mapper an {@code ObjectMapper}
     */
    public WebSocketRouter(final WebSocketEventService webSocketEventService, final ObjectMapper mapper) {
        this.webSocketEventService = webSocketEventService;
        this.mapper = mapper;
    }

    /**
     * Create a route from /resource/{token} to {@link WebSocketEventService#createFlowGraph(String token)}.
     * This method also handles serialising and deserialising the client's websocket stream from Akka
     * to our own {@link WebSocketMessage}.
     * Control over the rest of the client's request is handled by the flow graph.
     *
     * @return a {@link Route} for /resource/{token} which will handle websocket requests for filtered resources
     */
    @Override
    public Route get() {
        // ws://filtered-resource-service/resource/{token} -> serviceFactory.create(token) -> service.createFlowGraph()
        return Directives.pathPrefix("resource", () -> Directives.path(token -> Directives
                .handleWebSocketMessages(Flow.<Message>create()

                // Use text messages over binary messages (expecting JSON)
                .map(Message::asTextMessage)

                // Deserialise to WebsocketMessage class
                .flatMapConcat((TextMessage message) -> {
                    // Akka will sometimes convert between Strict and Streamed (if the websocket frames exceed ~5k 'PING' messages ~= 128KB)
                    Source<StringBuilder, NotUsed> builderSource;
                    if (message.isStrict()) {
                        // In case of a strict, just use the bounded-length string as expected
                        builderSource = Source.single(new StringBuilder(message.getStrictText()));
                    } else {
                        // In case of a stream, keep appending messages if they cross over separate frames
                        // This could cause an error if the client sends a malicious single message with a massive body
                        builderSource = message.getStreamedText()
                                .mapMaterializedValue(x -> NotUsed.notUsed())
                                .fold(new StringBuilder(), StringBuilder::append);
                    }
                    // Convert serialised data (JSON) to WebsocketMessage object
                    return builderSource.map(builder -> this.mapper.readValue(builder.toString(), WebSocketMessage.class));
                })

                // Process messages using service's flow graph
                .via(webSocketEventService.createFlowGraph(token))

                // Serialise back to TextMessage
                // This is not worth treating as a TextMessage.Streamed as under normal usage we're communicating with a single-message request-response
                .map(webSocketMessage -> new Strict(this.mapper.writeValueAsString(webSocketMessage))))
        ));
    }
}
