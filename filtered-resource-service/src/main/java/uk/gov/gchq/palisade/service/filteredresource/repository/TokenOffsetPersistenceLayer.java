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
package uk.gov.gchq.palisade.service.filteredresource.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persist and retrieve topic offsets for a given request token.
 * This allows creating a new kafka reader per request token and fast-forwarding to the start of the results.
 */
public interface TokenOffsetPersistenceLayer {

    /**
     * Put a token and its kafka offset into persistence if one does not exist.
     * If one does exist, it will not be overridden.
     *
     * @param token  the unique request token
     * @param offset the kafka commit offset for the start of results on the masked-resource input-topic
     * @return a future representing the completion of the async put operation
     */
    CompletableFuture<Void> putOffsetIfAbsent(final String token, final Long offset);

    /**
     * Put a token and its kafka offset into persistence, overwriting any that already exists for the token.
     *
     * @param token  the unique request token
     * @param offset the kafka commit offset for the start of results on the masked-resource input-topic
     * @return a future representing the completion of the async put operation
     */
    CompletableFuture<Void> overwriteOffset(final String token, final Long offset);

    /**
     * Find the offset of a given token if it is in the store.
     * Returns an {@link Optional}, empty if the token was not found, otherwise containing the offset
     *
     * @param token the unique request token
     * @return a future representing the completion of the async find operation
     */
    CompletableFuture<Optional<Long>> findOffset(final String token);

}
