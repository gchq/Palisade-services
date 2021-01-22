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

import akka.NotUsed;
import akka.stream.javadsl.Source;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level requirement for a database used for persistence, see {@link TypeEntity}
 * for more details
 */
public interface TypeRepository extends ReactiveCrudRepository<TypeEntity, String> {

    /**
     * Finds an entity using a resource id
     *
     * @param resourceId the id of the resource to retrieved
     * @return a {@link Mono} of {@link TypeEntity} from the backing store
     */
    Mono<TypeEntity> findOneByResourceId(String resourceId);

    /**
     * Checks if there is a {@link TypeEntity} for a resource id
     *
     * @param resourceId the id of the resource to be retrieved
     * @return a {@link CompletableFuture} of a {@link Boolean} if a TypeEntity is returned
     */
    default CompletableFuture<Boolean> futureExistsByResourceId(String resourceId) {
        return this.findOneByResourceId(resourceId).hasElement().toFuture();
    }

    /**
     * A {@link Flux} of resources by type
     *
     * @param type the type of the Resource to retrieve
     * @return a {@link Flux} of TypeEntity from the backing store
     */
    Flux<TypeEntity> findAllByType(String type);

    /**
     * Converts the {@code findAllByType} result to an akka {@link Source}
     *
     * @param type the type of the resource to retrieve
     * @return a {@link Source} of the returned {@link TypeEntity}s
     */
    default Source<TypeEntity, NotUsed> streamFindAllByType(String type) {
        return Source.fromPublisher(this.findAllByType(type));
    }

    /**
     * Saves (aka inserts) the object into the backing store via a {@link CrudRepository}
     *
     * @param entity Information about the resource Object
     * @return the {@link TypeEntity} that was saved in the backing store
     */
    default CompletableFuture<TypeEntity> futureSave(TypeEntity entity) {
        return this.save(entity).toFuture();
    }

}
