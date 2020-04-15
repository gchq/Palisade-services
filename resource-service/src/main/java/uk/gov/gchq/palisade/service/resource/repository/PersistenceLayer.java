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
 */
public interface PersistenceLayer {

    Optional<Stream<LeafResource>> getResourcesById(String resourceId);

    Optional<Stream<LeafResource>> getResourcesByType(String type);

    Optional<Stream<LeafResource>> getResourcesBySerialisedFormat(String serialisedFormat);


    void putResourcesById(String rootResourceId, LeafResource leafResource);

    void putResourcesByType(String type, LeafResource resource);

    void putResourcesBySerialisedFormat(String serialisedFormat, LeafResource resource);


    void addResource(LeafResource leafResource);

}
