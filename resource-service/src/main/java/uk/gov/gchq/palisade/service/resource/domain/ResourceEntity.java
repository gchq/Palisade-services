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

package uk.gov.gchq.palisade.service.resource.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.Resource;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * The Database uses this as the object that will be stored in the backing store linked by an ID
 * In this case the ResourceID and ResourceEntity make up the key
 * This contains all objects that will be go into the database, including how they are serialised and indexed
 */
@Entity
@Table(name = "resources",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "resource_id")
        },
        indexes = {
                @Index(name = "resource_id", columnList = "resource_id", unique = true),
                @Index(name = "parent_id", columnList = "parent_id"),
        })
@RedisHash("ResourceEntity")
public class ResourceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @org.springframework.data.annotation.Id
    @Indexed
    @Column(name = "resource_id", columnDefinition = "varchar(255)", nullable = false)
    private String resourceId;

    @Indexed
    @Column(name = "parent_id", columnDefinition = "varchar(255)")
    private String parentId;

    @Column(name = "resource", columnDefinition = "clob", nullable = false)
    @Convert(converter = ResourceConverter.class)
    private Resource resource;

    public ResourceEntity() {
    }

    private ResourceEntity(final String resourceId, final String parentId, final Resource resource) {
        this.resourceId = resourceId;
        this.parentId = parentId;
        this.resource = resource;
    }

    /**
     * Constructor used for the Database that takes a Resource and extracts the values
     * Used for inserting objects into the backing store
     *
     * @param resource specified to insert into the backing store
     */
    @PersistenceConstructor
    public ResourceEntity(final Resource resource) {
        this(
                resource.getId(),
                resource instanceof ChildResource ? ((ChildResource) resource).getParent().getId() : null,
                resource
        );
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public String getParentId() {
        return parentId;
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
