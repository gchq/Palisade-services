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

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;

import java.util.concurrent.CompletableFuture;

/**
 * When a client connects via websocket, the {@link FilteredResourceService} spawns an instance of the
 * {@link WebsocketEventService} to handle the rest of the request.
 * The service goes through the following steps while returning resources:
 * - get the topic offset for this token, defaulting to "now"
 * - send any "early" errors to the client (eg. user-service exceptions)
 * - send all appropriate masked resources to the client using the pre-calculated commit offset
 * - send any "late" errors to the client (eg. resource-service or policy-service exceptions)
 */
public class WebsocketEventService {
    private final TokenOffsetPersistenceLayer persistenceLayer;

    /**
     * Default constructor for a new WebsocketEventService, supplying the persistence layer for retrieving token offsets.
     * This will continually listen to a client's websocket for RTS/CTS handshakes, sending either errors or resources
     * back to the client as required.
     *
     * @param persistenceLayer the persistence layer for retrieving token offsets
     */
    public WebsocketEventService(final TokenOffsetPersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Retrieve the offset for a token from persistence, or default to kafka's 'now' offset for the partition.
     *
     * @param token the token to request an offset for
     * @return a future representing the asynchronous completion of the request
     * @apiNote the future should be completed before progressing further with the service's tasks
     * or else it may cause a race condition where early messages in the stream are dropped and lost
     */
    public CompletableFuture<Long> getTokenOffset(final String token) {
        return this.persistenceLayer.findOffset(token)
                // TODO: get the kafka offset for 'now' instead of this placeholder value
                .thenApply(offset -> offset.orElse(-1L));
    }

}
