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

package uk.gov.gchq.palisade.service.resource.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.gov.gchq.palisade.resource.ParentResource;

/**
 * The ResourceRepository stores indexable fields and a JSON CLOB for all persisted resources.
 * This includes, for any given persisted resource, its parent id, which will also be persisted.
 * Erase parent when storing JSON, it will be rebuilt using the repository.
 */
// Must be abstract class, not interface, to be used as mixin
@SuppressWarnings("java:S1610")
public abstract class OrphanedChildJsonMixin {
    /**
     * Ignore the parent field when serialising to JSON.
     *
     * @return irrelevant
     */
    @JsonIgnore
    abstract ParentResource getParent();

    /**
     * Ignore the parent field when deserialising from JSON.
     *
     * @param resource irrelevant
     */
    @JsonIgnore
    abstract void setParent(ParentResource resource);
}
