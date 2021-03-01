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

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessagePersistenceLayer;

import java.util.concurrent.CompletableFuture;

/**
 * A thread will constantly monitor a kafka queue throughout the lifetime of the application.
 * This queue recieves {@link AuditErrorMessage}(s) for a unique token.
 * When such a message is received, it will be persisted.
 * It will be later sent to the client via websocket.
 */
public class AuditErrorMessageEventService {
    private final TokenAuditErrorMessagePersistenceLayer persistenceLayer;

    /**
     * Default constructor for a new AuditErrorMessageEventService, supplying the persistence layer for storing tokens and the releated exception.
     * This will continually listen to kafka for {@link AuditErrorMessage}(s) to communicate the exception to any
     * running {@link WebSocketEventService}s as necessary.
     *
     * @param persistenceLayer the persistence layer for storing token and exceptions
     */
    public AuditErrorMessageEventService(final TokenAuditErrorMessagePersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Store the {@link TokenAuditErrorMessageEntity} containing the token and the exception from the error kafka queue
     *
     * @param token the unique request token
     * @return a {@link CompletableFuture} of a {@link TokenAuditErrorMessageEntity} representing the async completion of the store request
     */
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final AuditErrorMessage auditErrorMessage) {
        return this.persistenceLayer.putAuditErrorMessage(token, auditErrorMessage.getResourceId(), auditErrorMessage.getUserId(), auditErrorMessage.getContext(), auditErrorMessage.getAttributes(), auditErrorMessage.getError());
    }
}
