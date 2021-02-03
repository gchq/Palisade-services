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
import reactor.core.publisher.Mono;

import uk.gov.gchq.palisade.service.resource.domain.CompletenessEntity;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level requirement for a database used for persistence, see {@link CompletenessEntity}
 * for more details
 */
public interface CompletenessRepository extends ReactiveCrudRepository<CompletenessEntity, Integer> {

    /**
     * Boolean value returned based on whether a resource exits in the backing store by hashing the entityType and Id
     *
     * @param entityType Information about the resource object
     * @param entityId   the Id of the entity
     * @return true/false based on if the object exists in the backing store
     */
    Mono<CompletenessEntity> findOneByEntityTypeAndEntityId(EntityType entityType, String entityId);

    /**
     * Converts the {@code findOneByEntityTypeAndEntityId} result to an akka {@link Source}
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     * @return an akka {@link Source} of the {@link CompletenessEntity} from the backing store
     */
    default Source<CompletenessEntity, NotUsed> streamFindOneByEntityTypeAndEntityId(EntityType entityType, String entityId) {
        return Source.fromPublisher(this.findOneByEntityTypeAndEntityId(entityType, entityId));
    }

    /**
     * Checks if there is a {@link CompletenessEntity} using the passed parameters
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     * @return a {@link CompletableFuture} of a {@link Boolean} if a CompletenessEntity is returned from the backing store
     */
    default CompletableFuture<Boolean> futureExistsByEntityTypeAndEntityId(EntityType entityType, String entityId) {
        return this.findOneByEntityTypeAndEntityId(entityType, entityId).hasElement().toFuture();
    }

    /**
     * Saves (aka inserts) the object into the backing store via a {@link CrudRepository}
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     * @return the {@link CompletenessEntity} that was saved in the backing store
     */
    default Mono<CompletenessEntity> save(EntityType entityType, String entityId) {
        return this.save(new CompletenessEntity(entityType, entityId));
    }

    /**
     * Converts the saved entity to a {@link CompletableFuture}
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     * @return a {@link CompletableFuture} of the {@link CompletenessEntity} that has been saved
     */
    default CompletableFuture<CompletenessEntity> futureSave(EntityType entityType, String entityId) {
        return this.save(new CompletenessEntity(entityType, entityId)).toFuture();
    }

    /**
     * Converts the saved entity to an akka {@link Source}
     *
     * @param entityType Information about the resource Object
     * @param entityId   The Id of the entity
     * @return an akka {@link Source} of the {@link CompletenessEntity} that has been saved
     */
    default Source<CompletenessEntity, NotUsed> streamSave(EntityType entityType, String entityId) {
        return Source.fromPublisher(this.save(new CompletenessEntity(entityType, entityId)));
    }

}
