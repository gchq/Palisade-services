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
 * A thread will constantly monitor a kafka queue throughout the lifetime of the application.
 * This queue declares the commit-offsets for the starts of result sets for a given token.
 * When such a message is received, it will be persisted.
 * It will be later retrieved for a client's websocket.
 */
public class OffsetEventService {
    private final TokenOffsetPersistenceLayer persistenceLayer;

    /**
     * Default constructor for a new OffsetEventService, supplying the persistence layer for storing token offsets.
     * This will continually listen to kafka for START-OF-STREAM messages to communicate the commit-offsets to any
     * running {@link WebsocketEventService}s as necessary.
     *
     * @param persistenceLayer the persistence layer for storing token offsets
     */
    public OffsetEventService(final TokenOffsetPersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Store the commit offset for a token.
     *
     * @param token  the unique request token
     * @param offset the kafka topic commit offset fo the START-OF-STREAM message for the token
     * @return a {@link CompletableFuture} representing the async completion of the store request
     */
    public CompletableFuture<Void> storeTokenOffset(final String token, final Long offset) {
        return this.persistenceLayer.putOffsetIfAbsent(token, offset);
    }

}
