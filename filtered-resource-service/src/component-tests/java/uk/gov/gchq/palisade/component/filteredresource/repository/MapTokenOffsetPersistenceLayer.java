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

package uk.gov.gchq.palisade.component.filteredresource.repository;

import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Map-based implementation of persistence layer for testing purposes
 */
public class MapTokenOffsetPersistenceLayer implements TokenOffsetPersistenceLayer {
    final Map<String, Long> offsets = new HashMap<>();

    @Override
    public CompletableFuture<Void> putOffsetIfAbsent(final String token, final Long offset) {
        offsets.putIfAbsent(token, offset);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> overwriteOffset(final String token, final Long offset) {
        offsets.put(token, offset);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<Long>> findOffset(final String token) {
        return CompletableFuture.completedFuture(Optional.ofNullable(offsets.get(token)));
    }
}