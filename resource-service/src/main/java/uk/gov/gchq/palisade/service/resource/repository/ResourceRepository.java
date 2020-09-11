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

import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Low-level requirement for a database used for persistence, see {@link ResourceEntity}
 * for more details
 */
public interface ResourceRepository extends CrudRepository<ResourceEntity, String> {

    /**
     * Find resource in backing store by ResourceId
     *
     * @param resourceId the resource id of the resource in the backing store
     * @return Optional value of ResourceEntity stored in the backing store
     */
    Optional<ResourceEntity> findByResourceId(String resourceId);

    /**
     * Returns a stream of Resources from a backing store by ParentId
     *
     * @param parentId the parent id of the Resource
     * @return a stream of ResourceEntity resources from the backing store
     */
    default Stream<ResourceEntity> streamFindAllByParentId(String parentId) {
        return StreamSupport.stream(findAllByParentId(parentId).spliterator(), false);
    }

    /**
     * Iterable used to create a stream of resources by parentId
     *
     * @param parentId the parent id of the Resource
     * @return a list of ResourceEntity resources from the backing store
     */
    Iterable<ResourceEntity> findAllByParentId(String parentId);

}
