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
package uk.gov.gchq.palisade.service.data.repository;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity.AuthorisedRequestEntityId;

import java.util.Optional;

public interface AuthorisedRequestsRepository extends CrudRepository<AuthorisedRequestEntity, String> {


    default Optional<AuthorisedRequestEntity> findByEntityId(final AuthorisedRequestEntityId entityId) {
        return this.findById(entityId.getUniqueId());
    }

    default Optional<AuthorisedRequestEntity> findByTokenAndResourceId(final String token, final String resourceId) {
        return this.findByEntityId(new AuthorisedRequestEntityId(token, resourceId));
    }

}
