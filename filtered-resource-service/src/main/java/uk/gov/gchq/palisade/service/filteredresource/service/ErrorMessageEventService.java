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

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessagePersistenceLayer;

import java.util.concurrent.CompletableFuture;

/**
 * A thread will constantly monitor a kafka queue throughout the lifetime of the application.
 * This queue receives {@link AuditErrorMessage}(s) for a unique token.
 * When such a message is received, it will be persisted.
 * It will be later sent to the client via websocket.
 */
public class ErrorMessageEventService {
    private final TokenErrorMessagePersistenceLayer persistenceLayer;

    /**
     * Default constructor for a new ErrorMessageEventService, supplying the persistence layer for storing tokens and the releated exception.
     * This will continually listen to kafka for {@link AuditErrorMessage}(s) to communicate the exception to any
     * running {@link WebSocketEventService}s as necessary.
     *
     * @param persistenceLayer the persistence layer for storing token and exceptions
     */
    public ErrorMessageEventService(final TokenErrorMessagePersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Store the {@link TokenErrorMessageEntity} containing the token and the exception from the error kafka queue
     *
     * @param token             the unique request token
     * @param auditErrorMessage a AuditErrorMessage generated from an exception thrown in a different service
     * @return a {@link CompletableFuture} of a {@link TokenErrorMessageEntity} representing the async completion of the persistence event
     */
    public CompletableFuture<TokenErrorMessageEntity> putAuditErrorMessage(final String token, final AuditErrorMessage auditErrorMessage) {
        return this.persistenceLayer.putErrorMessage(token, auditErrorMessage.getServiceName(), auditErrorMessage.getError());
    }
}
