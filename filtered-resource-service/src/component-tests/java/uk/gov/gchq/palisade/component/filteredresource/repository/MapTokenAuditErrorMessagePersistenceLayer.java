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

import akka.japi.Pair;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.CrudRepositoryPop;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessagePersistenceLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Map-based implementation of persistence layer for testing purposes
 */
public class MapTokenAuditErrorMessagePersistenceLayer implements TokenAuditErrorMessagePersistenceLayer {
    final Map<String, AuditErrorMessage> tokenAuditErrorMessage = new HashMap<>();

    @Override
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final String resourceId, final String userId, final Context context, final Map<String, String> attributes, final Throwable error) {
        var auditErrorMessage = AuditErrorMessage.Builder.create().withUserId(userId).withResourceId(resourceId).withContext(context).withAttributes(attributes).withError(error);
        return CompletableFuture.completedFuture(new TokenAuditErrorMessageEntity(token, tokenAuditErrorMessage.putIfAbsent(token, auditErrorMessage)));
    }

    @Override
    public CompletableFuture<Optional<List<TokenAuditErrorMessageEntity>>> getAllAuditErrorMessages(final String token) {
        Map<String, AuditErrorMessage> mapOfMatchingTokensAndAuditErrorMessages = tokenAuditErrorMessage
                .entrySet()
                .stream()
                .filter(map -> token.equals(map.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        List<TokenAuditErrorMessageEntity> tokenAuditErrorMessageEntities = new ArrayList<>();
        for (Map.Entry<String, AuditErrorMessage> entry : mapOfMatchingTokensAndAuditErrorMessages.entrySet()) {
            tokenAuditErrorMessageEntities.add(new TokenAuditErrorMessageEntity(entry.getKey(), entry.getValue()));
        }
        return CompletableFuture.completedFuture(Optional.of(tokenAuditErrorMessageEntities));
    }

    @Override
    public CompletableFuture<Optional<Pair<TokenAuditErrorMessageEntity, CrudRepositoryPop>>> popAuditErrorMessage(final String token) {
        var auditErrorMessage = this.tokenAuditErrorMessage.get(token);
        this.tokenAuditErrorMessage.remove(token);
        return CompletableFuture.completedFuture(Optional.of(new Pair<>(new TokenAuditErrorMessageEntity(token, auditErrorMessage.getResourceId(), auditErrorMessage.getUserId(), auditErrorMessage.getContext(), auditErrorMessage.getAttributes(), auditErrorMessage.getError()), null)));
    }
}