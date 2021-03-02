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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenAuditErrorMessagePersistenceLayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Map-based implementation of persistence layer for testing purposes
 */
public class MapTokenAuditErrorMessagePersistenceLayer implements TokenAuditErrorMessagePersistenceLayer {
    final Map<String, LinkedList<TokenAuditErrorMessageEntity>> errorMessageMap = new HashMap<>();

    @Override
    public CompletableFuture<TokenAuditErrorMessageEntity> putAuditErrorMessage(final String token, final String resourceId, final String userId, final Context context, final String serviceName,
                                                                                final Map<String, String> attributes, final Throwable error) {
        errorMessageMap.computeIfAbsent(token, t -> new LinkedList<>());
        TokenAuditErrorMessageEntity entity = new TokenAuditErrorMessageEntity(token, userId, resourceId, context, serviceName, attributes, error.getMessage());
        errorMessageMap.get(token).addLast(entity);

        return CompletableFuture.completedFuture(entity);
    }

    @Override
    public CompletableFuture<List<TokenAuditErrorMessageEntity>> getAllAuditErrorMessages(final String token) {
        return CompletableFuture.completedFuture(Collections.unmodifiableList(errorMessageMap.getOrDefault(token, new LinkedList<>())));
    }

    @Override
    public CompletableFuture<Void> deleteAll(final List<TokenAuditErrorMessageEntity> listAEM) {
        errorMessageMap.values().forEach(entityList -> entityList.removeAll(listAEM));
        return CompletableFuture.completedFuture(null);
    }
}