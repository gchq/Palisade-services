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
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import uk.gov.gchq.palisade.reader.common.resource.LeafResource;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for a persistence store to be used as a cache by the resource-service
 * Due to the hierarchical nature of file systems, it is important to carefully examine
 * the many edge-cases this presents, particularly around deciding whether the persistence
 * store has a set of resources that represents the 'complete' response that the
 * underlying service would produce, rather than a 'partial' set
 */
public interface PersistenceLayer {

    /**
     * Given a resource id, return all {@link LeafResource}s underneath it
     *
     * @param resourceId the resource id to query
     * @return {@link Source} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesById(String resourceId);

    /**
     * Given a type, return all leaf resources of that type
     *
     * @param type the type to query
     * @return {@link Source} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesByType(String type);

    /**
     * Given a serialised format, return all leaf resources of that serialised format
     *
     * @param serialisedFormat the serialised format to query
     * @return {@link Source} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesBySerialisedFormat(String serialisedFormat);

    /**
     * Add a {@link LeafResource} to persistence for a given resourceId
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param <T>            the type for the {@link Flow}
     * @param rootResourceId the resource id that was queried to return this {@link Flow} of resources
     * @return an {@link Flow} of {@link LeafResource}s added to the persistence
     */
    <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceById(String rootResourceId);

    /**
     * Add a {@link LeafResource} to persistence for a given type
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param <T>  the type for the {@link Flow}
     * @param type the file type that was queried to return this {@link Flow} of resources
     * @return an {@link Flow} of {@link LeafResource}s added to the persistence
     */
    <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceByType(String type);

    /**
     * Add a {@link LeafResource} to persistence for a given serialised format
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param <T>              the type for the {@link Flow}
     * @param serialisedFormat the serialised format that was queried to return this {@link Flow} of resources
     * @return a {@link Flow} of {@link LeafResource}s added to the persistence
     */
    <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceBySerialisedFormat(String serialisedFormat);

}
