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
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;

import java.util.Iterator;

/**
 * Low-level requirement for a database used for persistence, see {@link ResourceEntity}
 * for more details
 *
 * @implNote In the future consider changing this to a {@link org.springframework.data.repository.reactive.ReactiveCrudRepository}
 */
public interface ResourceRepository extends CrudRepository<ResourceEntity, String> {

    /**
     * Find resource in backing store by ResourceId
     *
     * @param resourceId the resource id of the resource in the backing store
     * @return Optional value of ResourceEntity stored in the backing store
     */
    Iterable<ResourceEntity> findByResourceId(String resourceId);

    /**
     * Returns an {@link Iterator} of Resources from a backing store by ParentId
     *
     * @param parentId the parent id of the Resource
     * @return an {@link Iterator} of ResourceEntity resources from the backing store
     */
    default FunctionalIterator<ResourceEntity> iterateFindAllByParentId(String parentId) {
        return FunctionalIterator.fromIterator(findAllByParentId(parentId).iterator());
    }

    /**
     * Iterator of a list of resources by parentId
     *
     * @param parentId the parent id of the Resource
     * @return a list of ResourceEntity resources from the backing store
     */
    Iterable<ResourceEntity> findAllByParentId(String parentId);

}
