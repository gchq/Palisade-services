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
package uk.gov.gchq.palisade.service.resource.repository;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level requirement for a database used for persistence, see {@link ResourceEntity}
 * for more details
 */
public interface ResourceRepository extends ReactiveCrudRepository<ResourceEntity, String> {

    /**
     * Find resource in backing store by ResourceId
     *
     * @param resourceId the resource id of the resource in the backing store
     * @return {@link Mono} value of ResourceEntity stored in the backing store
     */
    Mono<ResourceEntity> findOneByResourceId(String resourceId);

    /**
     * Converts the {@code findOneByResourceId} result to an akka {@link Source}
     *
     * @param resourceId the id of the Resource
     * @return a {@link Source} of the returned {@link ResourceEntity}
     */
    default Source<ResourceEntity, NotUsed> streamFindOneByResourceId(String resourceId) {
        return Source.fromPublisher(this.findOneByResourceId(resourceId));
    }

    /**
     * Checks if there is a {@link ResourceEntity} for a resource id
     *
     * @param resourceId the id of the Resource
     * @return a {@link CompletableFuture} of a {@link Boolean} if a ResourceEntity is returned
     */
    default CompletableFuture<Boolean> futureExistsByResourceId(String resourceId) {
        return this.findOneByResourceId(resourceId).hasElement().toFuture();
    }

    /**
     * A {@link Flux} of resources by parentId
     *
     * @param parentId the parent id of the Resource
     * @return a {@link Flux} of ResourceEntity resources from the backing store
     */
    Flux<ResourceEntity> findAllByParentId(String parentId);

    /**
     * Converts the {@code findAllByParentId} result to an akka {@link Source}
     *
     * @param parentId the parent id of the Resource
     * @return a {@link Source} of the returned {@link ResourceEntity}s
     */
    default Source<ResourceEntity, NotUsed> streamFindAllByParentId(String parentId) {
        return Source.fromPublisher(this.findAllByParentId(parentId));
    }

    /**
     * Saves (aka inserts) the object into the backing store via a {@link CrudRepository}
     *
     * @param entity Information about the resource Object
     * @return the {@link ResourceEntity} that was saved in the backing store
     */
    default Mono<ResourceEntity> futureSave(ResourceEntity entity) {
        return this.save(entity);
    }
}
