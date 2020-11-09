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
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;

import java.util.Iterator;

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
     * @return {@link Iterator} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    Iterator<LeafResource> getResourcesById(String resourceId);

    /**
     * Given a type, return all leaf resources of that type
     *
     * @param type the type to query
     * @return {@link Iterator} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    Iterator<LeafResource> getResourcesByType(String type);

    /**
     * Given a serialised format, return all leaf resources of that serialised format
     *
     * @param serialisedFormat the serialised format to query
     * @return {@link Iterator} of {@link LeafResource}s if the persistence store is aware of these resources
     */
    Iterator<LeafResource> getResourcesBySerialisedFormat(String serialisedFormat);

    /**
     * Add a {@link LeafResource} to persistence for a given resourceId
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param rootResourceId the resource id that was queried to return this stream of resources
     * @param resources      the resource stream returned
     * @return an {@link Iterator} of the {@link LeafResource}s added to the persistence
     */
    <T> FunctionalIterator<T> withPersistenceById(String rootResourceId, FunctionalIterator<T> resources);

    /**
     * Add a {@link LeafResource} to persistence for a given type
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param type      the type that was queried to return this stream of resources
     * @param resources the resource stream returned
     * @return an {@link Iterator} of the {@link LeafResource}s added to the persistence
     */
    <T> FunctionalIterator<T> withPersistenceByType(String type, FunctionalIterator<T> resources);

    /**
     * Add a {@link LeafResource} to persistence for a given serialised format
     * Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
     *
     * @param serialisedFormat the serialised format that was queried to return this stream of resources
     * @param resources        the resource stream returned
     * @return an {@link Iterator} of the {@link LeafResource}s added to the persistence
     */
    <T> FunctionalIterator<T> withPersistenceBySerialisedFormat(String serialisedFormat, FunctionalIterator<T> resources);


    /**
     * Add a new resource that has been created during runtime to the persistence store
     * This behaviour will otherwise invalidate the persistence store (it may still if desired in this method)
     * Used for updating the persistence store when the source of 'truth' has changed
     * As long as this is called for every new resource created and added to the resource-service,
     * this should guarantee consistency between persistence and resource-service
     *
     * @param leafResource the new {@link LeafResource} that has been created
     */
    void addResource(LeafResource leafResource);

}
