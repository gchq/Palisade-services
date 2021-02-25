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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persist and retrieve {@link AuditErrorMessage}(s) for a given request token.
 */
public interface TokenAuditErrorMessageRepository extends CrudRepository<TokenAuditErrorMessageEntity, String> {

    /**
     * Put a token and its {@link AuditErrorMessage} into persistence.
     * This is a ease-of-use wrapper around {@link CrudRepository#save}.
     *
     * @param token the unique request token
     * @return the newly saved {@link TokenAuditErrorMessageEntity} containing the excpetion and unique token
     */
    default TokenAuditErrorMessageEntity save(final String token, final String resourceId, final String userId, final Context context, final Map<String, String> attributes, final Throwable error) {
        return save(new TokenAuditErrorMessageEntity(token, resourceId, userId, context, attributes, error));
    }

    /**
     * Find all TokenAuditErrorMessageEntitys from the repository by the unique token.
     *
     * @param token the unique request token
     * @return an Optional of the {@link TokenAuditErrorMessageEntity} containing the exception and token
     */
    Optional<List<TokenAuditErrorMessageEntity>> findAllByToken(final String token);

    Optional<TokenAuditErrorMessageEntity> findFirstByToken(final String token);
}
