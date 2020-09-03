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

import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Low-level requirement for a database used for persistence, see {@link TypeEntity}
 * for more details
 */
public interface TypeRepository extends CrudRepository<TypeEntity, String> {

    /**
     * Returns a stream of {@link TypeEntity} from a backing store by a type
     *
     * @param type of resource to retrieve
     * @return a stream of TypeEntities from the backing store
     */
    default Stream<TypeEntity> streamFindAllByType(String type) {
        return StreamSupport.stream(findAllByType(type).spliterator(), false);
    }

    /**
     * Iterable used to create a stream of resources by serialisedFormat
     *
     * @param type the type of the Resource to retrieve
     * @return a list of TypeEntity from the backing store
     */
    Iterable<TypeEntity> findAllByType(String type);

}
