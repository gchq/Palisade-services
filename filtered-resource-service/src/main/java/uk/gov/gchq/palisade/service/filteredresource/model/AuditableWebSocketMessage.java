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
import akka.kafka.ConsumerMessage.Committable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.Generated;

/**
 * Collect a {@link WebSocketMessage} prepared to be returned to the client with any other context
 * required to correctly audit the message as a successful request or not (the original {@link Pair}
 * from the {@link uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory}).
 */
public class AuditableWebSocketMessage {
    private final WebSocketMessage websocketMessage;
    private final Committable committable;
    private final FilteredResourceRequest filteredResourceRequest;
    private final AuditErrorMessage auditErrorMessage;

    protected AuditableWebSocketMessage(
            final @NonNull WebSocketMessage websocketMessage,
            final @NonNull Committable committable,
            final @Nullable FilteredResourceRequest filteredResourceRequest,
            final @Nullable AuditErrorMessage auditErrorMessage
    ) {
        this.websocketMessage = websocketMessage;
        this.committable = committable;
        this.filteredResourceRequest = filteredResourceRequest;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public WebSocketMessage getWebSocketMessage() {
        return websocketMessage;
    }

    @Generated
    public WebSocketMessage getWebsocketMessage() {
        return websocketMessage;
    }

    @Generated
    public Committable getCommittable() {
        return committable;
    }

    @Generated
    public FilteredResourceRequest getFilteredResourceRequest() {
        return filteredResourceRequest;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
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
            return websocketMessage -> committable -> auditablePair ->
                    new AuditableWebSocketMessage(websocketMessage, committable, auditablePair.first(), auditablePair.second());
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
            ICommittable withWebSocketMessage(@NonNull WebSocketMessage websocketMessage);
        }

        public interface ICommittable {
            IAuditable withCommittable(Committable committable);

            default AuditableWebSocketMessage withoutCommittable() {
                return withCommittable(null).withAuditablePair(Pair.create(null, null));
            }
        }

        /**
         * Compose inbound request data
         */
        public interface IAuditable {
            AuditableWebSocketMessage withAuditablePair(@NonNull Pair<FilteredResourceRequest, AuditErrorMessage> auditablePair);

            default AuditableWebSocketMessage withFilteredResourceRequest(final @NonNull FilteredResourceRequest request) {
                return withAuditablePair(Pair.create(request, null));
            }

            default AuditableWebSocketMessage withAuditErrorMessage(final @NonNull AuditErrorMessage auditErrorMessage) {
                return withAuditablePair(Pair.create(null, auditErrorMessage));
            }

        }
    }
}
