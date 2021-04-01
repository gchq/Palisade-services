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
import org.springframework.data.domain.Persistable;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import uk.gov.gchq.palisade.service.resource.common.Generated;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * The Database uses this as the object that will be stored in the backing store linked by an ID
 * In this case the ResourceID and type make up the key
 * This contains all objects that will be go into the database, including how they are serialised and indexed
 */
@Table("types")
public class TypeEntity implements Serializable, Persistable<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @Column("resource_id")
    private final String resourceId;

    @Indexed
    @Column("type")
    private final String type;

    /**
     * Constructor used for the Database
     * Used for inserting objects into the backing store
     *
     * @param type       the type of resource that will be inserted into the backing store
     * @param resourceId the id of the resource that will be inserted into the backing store
     */
    @PersistenceConstructor
    @JsonCreator
    public TypeEntity(final @JsonProperty("type") String type,
                      final @JsonProperty("resourceId") String resourceId) {
        this.type = type;
        this.resourceId = resourceId;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return this.resourceId;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return true;
    }

    @Generated
    public String getType() {
        return type;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TypeEntity.class.getSimpleName() + "[", "]")
                .add("resourceId='" + resourceId + "'")
                .add("type='" + type + "'")
                .add(super.toString())
                .toString();
    }
}
