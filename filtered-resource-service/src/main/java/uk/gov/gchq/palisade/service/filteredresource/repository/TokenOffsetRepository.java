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

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenOffsetEntity;

import java.util.Optional;

/**
 * Persist and retrieve topic offsets for a given request token.
 * This allows creating a new kafka reader per request token and fast-forwarding to the start of the results.
 */
public interface TokenOffsetRepository extends CrudRepository<TokenOffsetEntity, String> {

    /**
     * Persist the {@code TokenOffsetEntity} to storage
     *
     * @param token that identifies the request
     * @param offset in the kafka consumer topic
     */
    default void save(final String token, final Long offset) {
        save(new TokenOffsetEntity(token, offset));
    }

    /**
     * Find the offset in the consumer topic for a given query token
     *
     * @param token that identifies the request
     * @return an optional wrapped {@code TokenOffsetEntity}
     */
    Optional<TokenOffsetEntity> findByToken(final String token);

}
