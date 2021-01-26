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

import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level requirement for a database used for persistence, see {@link SerialisedFormatEntity}
 * for more details
 */
public interface SerialisedFormatRepository extends ReactiveCrudRepository<SerialisedFormatEntity, String> {

    /**
     * Finds an entity using a resource id
     *
     * @param resourceId the id of the resource to retrieved
     * @return a {@link Mono} of {@link SerialisedFormatEntity} from the backing store
     */
    Mono<SerialisedFormatEntity> findOneByResourceId(String resourceId);

    /**
     * Checks if there is a {@link TypeEntity} for a resource id
     *
     * @param resourceId the id of the resource to be retrieved
     * @return a {@link CompletableFuture} of a {@link Boolean} if a SerialisedFormatEntity is returned
     */
    default CompletableFuture<Boolean> futureExistsFindOneByResourceId(String resourceId) {
        return this.findOneByResourceId(resourceId).hasElement().toFuture();
    }

    /**
     * A {@link Flux} of resources by serialisedFormat
     *
     * @param serialisedFormat the format of the Resource
     * @return a list of SerialisedFormatEntity from the backing store
     */
    Flux<SerialisedFormatEntity> findAllBySerialisedFormat(String serialisedFormat);

    /**
     * Converts the {@code findAllBySerialisedFormat} result to an akka {@link Source}
     *
     * @param serialisedFormat the format of the Resource
     * @return a {@link Source} of the returned {@link SerialisedFormatEntity}s
     */
    default Source<SerialisedFormatEntity, NotUsed> streamFindAllBySerialisedFormat(String serialisedFormat) {
        return Source.fromPublisher(this.findAllBySerialisedFormat(serialisedFormat));
    }

    /**
     * Saves (aka inserts) the object into the backing store via a {@link CrudRepository}
     *
     * @param entity Information about the resource Object
     * @return the {@link SerialisedFormatEntity} that was saved in the backing store
     */
    default CompletableFuture<SerialisedFormatEntity> futureSave(SerialisedFormatEntity entity) {
        return this.save(entity).toFuture();
    }
}
