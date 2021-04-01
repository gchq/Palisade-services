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

import uk.gov.gchq.palisade.service.filteredresource.common.Generated;

/**
 * Collect a {@link WebSocketMessage} prepared to be returned to the client with any other context
 * required to correctly audit the message as a successful request or not (the original {@link Pair}
 * from the {@link uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph.FilteredResourceSourceFactory}).
 */
public class AuditableWebSocketMessage {
    private final WebSocketMessage websocketMessage;
    private final FilteredResourceRequest filteredResourceRequest;
    private final Committable committable;

    protected AuditableWebSocketMessage(
            final @NonNull WebSocketMessage websocketMessage,
            final @Nullable FilteredResourceRequest filteredResourceRequest,
            final @Nullable Committable committable) {
        this.websocketMessage = websocketMessage;
        this.filteredResourceRequest = filteredResourceRequest;
        this.committable = committable;
    }

    @Generated
    public WebSocketMessage getWebSocketMessage() {
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
            return websocketMessage -> resourceAndComittable ->
                    new AuditableWebSocketMessage(websocketMessage, resourceAndComittable.first(), resourceAndComittable.second());
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
         * Adds the committable to the message
         */
        public interface IAuditable {

            /**
             * Supply a pair of {@link FilteredResourceRequest} and {@link Committable} to the builder
             *
             * @param resourceAndCommittable the pair, containing the original request information sent to the Filtered Resource Service, and the committable needed to commit upstream
             * @return a completed {@link AuditableWebSocketMessage} object
             */
            AuditableWebSocketMessage withResourceAndCommittable(@NonNull Pair<FilteredResourceRequest, Committable> resourceAndCommittable);

            /**
             * Supply both a {@link FilteredResourceRequest} and a {@link Committable} to the builder, which will join them as a pair.
             *
             * @param request     the request message that was sent to the Filtered Resource Service
             * @param committable needed to commit upstream
             * @return a completed {@link AuditableWebSocketMessage} object
             */
            default AuditableWebSocketMessage withResourceAndCommittable(@NonNull FilteredResourceRequest request, @NonNull Committable committable) {
                return withResourceAndCommittable(Pair.create(request, committable));
            }

            /**
             * By default, no committable exists to return nulls for the committable and AuditablePair
             *
             * @return a null comittable and a null auditablePair
             */
            default AuditableWebSocketMessage withoutAudit() {
                return withResourceAndCommittable(Pair.create(null, null));
            }
        }

    }
}
