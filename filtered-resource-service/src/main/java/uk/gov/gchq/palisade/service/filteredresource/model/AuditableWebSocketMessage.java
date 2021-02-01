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

package uk.gov.gchq.palisade.service.filteredresource.model;

import akka.japi.Pair;
import akka.kafka.ConsumerMessage.CommittableOffset;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.Generated;

import java.util.Optional;

/**
 * Collect a {@link WebSocketMessage} prepared to be returned to the client with any other context
 * required to correctly audit the message as a successful request or not (the original {@link Pair}
 * from the {@link uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory}).
 */
public class AuditableWebSocketMessage {
    private final WebSocketMessage websocketMessage;
    private final Pair<FilteredResourceRequest, CommittableOffset> auditSuccessPair;

    protected AuditableWebSocketMessage(
            final @NonNull WebSocketMessage websocketMessage,
            final @Nullable Pair<FilteredResourceRequest, CommittableOffset> auditSuccessPair
    ) {
        this.websocketMessage = websocketMessage;
        this.auditSuccessPair = auditSuccessPair;
    }

    @Generated
    public WebSocketMessage getWebSocketMessage() {
        return websocketMessage;
    }

    @Generated
    public Optional<Pair<FilteredResourceRequest, CommittableOffset>> getAuditSuccessPair() {
        return Optional.ofNullable(auditSuccessPair);
    }

    /**
     * Builder for {@link AuditableWebSocketMessage} objects, combining an outbound {@link WebSocketMessage} with its inbound {@link FilteredResourceRequest}.
     * This is later used to commit the inbound request to Kafka such that it will not be re-read.
     */
    public static class Builder {
        /**
         * Create a new builder
         *
         * @return the next step in the builder chain
         */
        public static IWebSocketMessage create() {
            return websocketMessage -> auditableRequest -> new AuditableWebSocketMessage(websocketMessage, auditableRequest);
        }

        /**
         * Compose outbound websocket message
         */
        public interface IWebSocketMessage {
            /**
             * Compose outbound websocket message
             *
             * @param websocketMessage the outbound websocket message
             * @return the next step in the builder
             */
            IAuditable withWebSocketMessage(@NonNull WebSocketMessage websocketMessage);
        }

        /**
         * Compose inbound request data
         */
        public interface IAuditable {
            /**
             * Compose inbound request data
             *
             * @param auditableRequest a pair of the inbound request and its offset
             * @return a completed AuditableWebSocketMessage
             */
            AuditableWebSocketMessage withAuditablePair(@Nullable Pair<FilteredResourceRequest, CommittableOffset> auditableRequest);

            /**
             * Compose inbound request data
             *
             * @param request           the inbound request
             * @param committableOffset the inbound request's kafka offset to be committed back to kafka once done
             * @return a completed AuditableWebSocketMessage
             */
            default AuditableWebSocketMessage withAuditable(final @NonNull FilteredResourceRequest request, final @NonNull CommittableOffset committableOffset) {
                return withAuditablePair(Pair.create(request, committableOffset));
            }

            /**
             * Don't add any audit information (e.g. an {@link AuditableWebSocketMessage} for a {@link MessageType#PING})
             *
             * @return a completed AuditableWebSocketMessage
             */
            default AuditableWebSocketMessage withoutAuditable() {
                return withAuditablePair(null);
            }
        }
    }
}
