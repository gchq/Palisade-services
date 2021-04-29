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
package uk.gov.gchq.palisade.service.data.repository;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity.AuthorisedRequestEntityId;

import java.util.Optional;

/**
 * Read-only repository interface for reading the details of an authorised request using its token and leaf resource id.
 * This database is written to by the Attribute-Masking Service.
 */
public interface AuthorisedRequestsRepository extends CrudRepository<AuthorisedRequestEntity, String> {

    /**
     * Find an {@link AuthorisedRequestEntity} by its unique {@link AuthorisedRequestEntityId}
     * This id is a combination of the token and leaf resource id.
     *
     * @param entityId the entity's unique id object
     * @return an {@link Optional} of whether the entity was found.
     */
    default Optional<AuthorisedRequestEntity> findByEntityId(final AuthorisedRequestEntityId entityId) {
        return this.findById(entityId.getUniqueId());
    }

    /**
     * Find an {@link AuthorisedRequestEntity} by its unique combination of token and leaf resource id.
     *
     * @param token      the client's request token
     * @param resourceId the leaf resource id the client requested
     * @return an {@link Optional} of whether the entity was found
     */
    default Optional<AuthorisedRequestEntity> findByTokenAndResourceId(final String token, final String resourceId) {
        return this.findByEntityId(new AuthorisedRequestEntityId(token, resourceId));
    }

}
