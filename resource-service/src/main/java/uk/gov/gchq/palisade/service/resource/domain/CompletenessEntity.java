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
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import uk.gov.gchq.palisade.service.resource.common.Generated;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * The Database uses this as the object that will be stored in the backing store linked by an ID
 * In this case the Id_pair_hash is the ID which is created by creating a hash of the entity_type object and entity_id
 * This contains all objects that will be go into the database, including how they are serialised and indexed
 */
@Table("completeness")
public class CompletenessEntity implements Serializable, Persistable<Integer> {
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private final int id;

    @Column("entity_type")
    private final EntityType entityType;

    @Column("entity_id")
    private final String entityId;

    /**
     * Constructor used for the Database that takes a {@link EntityType} and EntityId
     * Used for inserting objects into the backing store
     * A primary key is created by hashing the entityType and Id into a unique ID
     *
     * @param entityType The type of entity, see {@link EntityType}.
     * @param entityId   The Id of the entity.
     */
    public CompletenessEntity(final EntityType entityType, final String entityId) {
        this(entityType, entityId, idFor(entityType, entityId));
    }

    /**
     * Constructor used for the Database that takes json
     * Used for inserting objects into the backing store
     *
     * @param entityType the Entity type enum object
     * @param entityId   The Id of the entity, which eventually becomes a hash of the type and Id as a primary key
     * @param id         the id of the stored entity
     */
    @PersistenceConstructor
    @JsonCreator
    public CompletenessEntity(final @JsonProperty("entityType") EntityType entityType,
                              final @JsonProperty("entityId") String entityId,
                              final @JsonProperty("id") int id) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.id = id;
    }

    @Generated
    public Integer getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return true;
    }

    @Generated
    public EntityType getEntityType() {
        return entityType;
    }

    @Generated
    public String getEntityId() {
        return entityId;
    }

    /**
     * Creates a unique value for the passed in parameters
     *
     * @param entityType the Entity type enum object
     * @param entityId   The Id of the entity, which eventually becomes a hashCode
     * @return the hash value used for the id, made up of the length of EntityType enum,
     * times the hashCode of entityId and the ordinal value of the passed in type
     */
    public static Integer idFor(final EntityType entityType, final String entityId) {
        return EntityType.values().length * entityId.hashCode() + entityType.ordinal();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompletenessEntity)) {
            return false;
        }
        final CompletenessEntity that = (CompletenessEntity) o;
        return entityType == that.entityType &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(entityType, entityId);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", CompletenessEntity.class.getSimpleName() + "[", "]")
                .add("entityType=" + entityType)
                .add("entityId='" + entityId + "'")
                .add(super.toString())
                .toString();
    }
}
