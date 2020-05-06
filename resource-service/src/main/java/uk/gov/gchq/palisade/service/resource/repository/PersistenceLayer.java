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

import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.Optional;
import java.util.stream.Stream;

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
     * @return Optional.of a {@link Stream} of {@link LeafResource}s if the persistence store is aware of these resources
     *         Optional.empty if no such information has been persisted
     * nb. the {@link Stream} may still be empty if this resourceId is an empty directory
     */
    Optional<Stream<LeafResource>> getResourcesById(String resourceId);

    /**
     * Given a type, return all leaf resources of that type
     *
     * @param type the type to query
     * @return Optional.of a {@link Stream} of {@link LeafResource}s if the persistence store is aware of these resources
     *         Optional.empty if no such information has been persisted
     * nb. the {@link Stream} may still be empty if this type simply has no resources matching it
     */
    Optional<Stream<LeafResource>> getResourcesByType(String type);

    /**
     * Given a serialised format, return all leaf resources of that serialised format
     *
     * @param serialisedFormat the serialised format to query
     * @return Optional.of a {@link Stream} of {@link LeafResource}s if the persistence store is aware of these resources
     *         Optional.empty if no such information has been persisted
     * nb. the {@link Stream} may still be empty if this serialised format simply has no resources matching it
     */
    Optional<Stream<LeafResource>> getResourcesBySerialisedFormat(String serialisedFormat);

    /**
     * Add a {@link Stream} of {@link LeafResource}s to persistence for a given resourceId
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param rootResourceId the resource id that was queried to return this stream of resources
     * @param resources      the resource stream returned
     * @return a new stream which will persist each resource as it is consumed
     */
    Stream<LeafResource> withPersistenceById(String rootResourceId, Stream<LeafResource> resources);

    /**
     * Add a {@link Stream} of {@link LeafResource}s to persistence for a given type
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param type      the type that was queried to return this stream of resources
     * @param resources the resource stream returned
     * @return a new stream which will persist each resource as it is consumed
     */
    Stream<LeafResource> withPersistenceByType(String type, Stream<LeafResource> resources);

    /**
     * Add a {@link Stream} of {@link LeafResource}s to persistence for a given serialised format
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param serialisedFormat the serialised format that was queried to return this stream of resources
     * @param resources        the resource stream returned
     * @return a new stream which will persist each resource as it is consumed
     */
    Stream<LeafResource> withPersistenceBySerialisedFormat(String serialisedFormat, Stream<LeafResource> resources);


    /**
     * Add a new resource that has been created during runtime to the persistence store
     * This behaviour will otherwise invalidate the persistence store (it may still if desired in this method)
     * Used for updating the persistence store when the source of 'truth' has changed
     *
     * As long as this is called for every new resource created and added to the resource-service,
     * this should guarantee consistency between persistence and resource-service
     *
     * @param leafResource the new {@link LeafResource} that has been created
     */
    void addResource(LeafResource leafResource);

}
