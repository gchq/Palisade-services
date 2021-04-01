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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.reader.common.resource.ChildResource;
import uk.gov.gchq.palisade.reader.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.common.Generated;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * The Database uses this as the object that will be stored in the backing store linked by an ID
 * In this case the ResourceID and ResourceEntity make up the key
 * This contains all objects that will be go into the database, including how they are serialised and indexed
 */
@Table("resources")
public class ResourceEntity implements Serializable, Persistable<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @Indexed
    @Column("resource_id")
    private final String resourceId;

    @Indexed
    @Column("parent_id")
    private final String parentId;

    @Column("resource")
    private final Resource resource;

    @Transient
    private final boolean isNew;

    @PersistenceConstructor
    @JsonCreator
    private ResourceEntity(final @JsonProperty("resourceId") String resourceId,
                           final @JsonProperty("parentId") String parentId,
                           final @JsonProperty("resource") Resource resource) {
        this(resourceId, parentId, resource, false);
    }

    private ResourceEntity(final String resourceId, final String parentId, final Resource resource, final boolean isNew) {
        this.resourceId = resourceId;
        this.parentId = parentId;
        this.resource = resource;
        this.isNew = isNew;
    }

    /**
     * Constructor used for the Database that takes a Resource and extracts the values
     * Used for inserting objects into the backing store
     *
     * @param resource specified to insert into the backing store
     */
    public ResourceEntity(final Resource resource) {
        this(
                resource.getId(),
                resource instanceof ChildResource ? ((ChildResource) resource).getParent().getId() : null,
                resource,
                true
        );
    }

    @Override
    @JsonIgnore
    public String getId() {
        return resourceId;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @Generated
    @Nullable
    public String getParentId() {
        return parentId;
    }

    @Generated
    public String getResourceId() {
        return getId();
    }

    @Generated
    public Resource getResource() {
        return resource;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceEntity.class.getSimpleName() + "[", "]")
                .add("resourceId='" + resourceId + "'")
                .add("parentId='" + parentId + "'")
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }
}
