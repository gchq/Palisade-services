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

package uk.gov.gchq.palisade.service.resource.repository;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.resource.domain.CompletenessEntity;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;

/**
 * Low-level requirement for a database used for persistence, see {@link CompletenessEntity}
 * for more details
 */
public interface CompletenessRepository extends CrudRepository<CompletenessEntity, Integer> {

    /**
     * Boolean value returned based on whether a resource exits in the backing store by hashing the entityType and Id
     *
     * @param entityType Information about the resource object
     * @param entityId   the Id of the entity
     * @return true/false based on if the object exists in the backing store
     */
    default boolean compositeExistsByEntityTypeAndEntityId(EntityType entityType, String entityId) {
        return existsById(new CompletenessEntity(entityType, entityId).hashCode());
    }

    boolean existsById(Integer id);

    /**
     * Saves or inserts the object into the backing store via a {@link CrudRepository}
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     */
    default void save(EntityType entityType, String entityId) {
        save(new CompletenessEntity(entityType, entityId));
    }

}
