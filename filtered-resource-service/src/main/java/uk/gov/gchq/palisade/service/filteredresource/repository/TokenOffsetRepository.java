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
     * Put a token and its kafka offset into persistence.
     * This is a ease-of-use wrapper around {@link CrudRepository#save}.
     *
     * @param token  the unique request token
     * @param offset the kafka commit offset for the start of results on the masked-resource input-topic
     */
    default void save(final String token, final Long offset) {
        save(new TokenOffsetEntity(token, offset));
    }

    /**
     * Find the entity (ie the kafka offset) for a token from persistence.
     *
     * @param token the unique request token
     * @return an {@link Optional}, present if the token was found, empty otherwise
     */
    Optional<TokenOffsetEntity> findByToken(final String token);

}
