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
package uk.gov.gchq.palisade.service.filteredresource.repository.exception;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;

import java.util.List;

/**
 * Persists and retrieves {@link TokenErrorMessageEntity}(s) for a given request token.
 */
public interface TokenErrorMessageRepository extends CrudRepository<TokenErrorMessageEntity, String> {

    /**
     * Put a token, the name of the service that threw the exception, and its error message into persistence
     * This is a ease-of-use wrapper around {@link CrudRepository#save}.
     *
     * @param token       the unique request token
     * @param serviceName the name of the service that the error was thrown from
     * @param error       the error thrown in the service
     * @return a new {@link TokenErrorMessageEntity} containing the object saved to persistence
     */
    default TokenErrorMessageEntity save(final String token, final String serviceName, final Throwable error) {
        return save(new TokenErrorMessageEntity(token, serviceName, error.getMessage()));
    }

    /**
     * Find all TokenErrorMessageEntities from the repository by the unique token.
     *
     * @param token the unique request token
     * @return an Optional of the {@link TokenErrorMessageEntity} containing the exception and token
     */
    List<TokenErrorMessageEntity> findAllByToken(final String token);
}
