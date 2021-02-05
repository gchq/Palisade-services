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

package uk.gov.gchq.palisade.component.filteredresource.repository;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessagePersistenceLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Map-based implementation of persistence layer for testing purposes
 */
public class MapTokenAuditErrorMessagePersistenceLayer implements TokenAuditErrorMessagePersistenceLayer {
    final Map<String, AuditErrorMessage> tokenAuditErrorMessage = new HashMap<>();

    @Override
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final AuditErrorMessage auditErrorMessage) {
        return CompletableFuture.completedFuture(new TokenAuditErrorMessageEntity(token, tokenAuditErrorMessage.putIfAbsent(token, auditErrorMessage)));
    }

    @Override
    public CompletableFuture<Optional<AuditErrorMessage>> popAuditErrorMessage(final String token) {
        var auditErrorMessage = this.tokenAuditErrorMessage.get(token);
        this.tokenAuditErrorMessage.remove(token);
        return CompletableFuture.completedFuture(Optional.ofNullable(auditErrorMessage));
    }
}