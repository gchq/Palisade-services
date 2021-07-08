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

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.repository.error.TokenErrorMessagePersistenceLayer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Map-based implementation of persistence layer for testing purposes
 */
public class MapTokenErrorMessagePersistenceLayer implements TokenErrorMessagePersistenceLayer {
    final Map<String, LinkedList<TokenErrorMessageEntity>> errorMessageMap = new HashMap<>();

    @Override
    public CompletableFuture<TokenErrorMessageEntity> putErrorMessage(final String token, final String serviceName, final Throwable error) {
        errorMessageMap.computeIfAbsent(token, t -> new LinkedList<>());
        TokenErrorMessageEntity entity = new TokenErrorMessageEntity(token, serviceName, error.getMessage());
        errorMessageMap.get(token).addLast(entity);

        return CompletableFuture.completedFuture(entity);
    }

    @Override
    public CompletableFuture<List<TokenErrorMessageEntity>> getAllErrorMessages(final String token) {
        return CompletableFuture.completedFuture(new LinkedList<>(errorMessageMap.getOrDefault(token, new LinkedList<>())));
    }

    @Override
    public CompletableFuture<Void> deleteAll(final List<TokenErrorMessageEntity> messageEntityList) {
        errorMessageMap.values().forEach(entityList -> entityList.removeAll(messageEntityList));
        return CompletableFuture.completedFuture(null);
    }
}