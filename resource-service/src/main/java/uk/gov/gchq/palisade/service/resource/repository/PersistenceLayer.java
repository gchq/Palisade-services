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
import uk.gov.gchq.palisade.service.resource.web.ResourceController;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * High-level requirement for persistence - approximately mirrors the external
 * interface with the {@link ResourceController}.
 *
 * Get/Put  byResource excluded here as it would still require a persistence lookup
 * to check completeness, making it equivalent to byId.
 */
public interface PersistenceLayer {

    Optional<Stream<LeafResource>> getResourcesById(String resourceId);

    Optional<Stream<LeafResource>> getResourcesByType(String type);

    Optional<Stream<LeafResource>> getResourcesBySerialisedFormat(String serialisedFormat);


    Stream<LeafResource> withPersistenceById(String rootResourceId, Stream<LeafResource> resources);

    Stream<LeafResource> withPersistenceByType(String type, Stream<LeafResource> resources);

    Stream<LeafResource> withPersistenceBySerialisedFormat(String serialisedFormat, Stream<LeafResource> resources);


    void addResource(LeafResource leafResource);

}
