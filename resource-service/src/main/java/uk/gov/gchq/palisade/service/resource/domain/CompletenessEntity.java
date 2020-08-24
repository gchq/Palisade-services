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

import uk.gov.gchq.palisade.Generated;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@IdClass(CompletenessEntity.CompletenessId.class)
@Table(name = "completeness",
        indexes = {
                @Index(name = "entity_type", columnList = "entity_type"),
                @Index(name = "entity_id", columnList = "entity_id"),
        })
public class CompletenessEntity {
    @Id
    @Column(name = "entity_type", columnDefinition = "integer", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    protected EntityType entityType;

    @Id
    @Column(name = "entity_id", columnDefinition = "varchar(255)", nullable = false)
    protected String entityId;

    public CompletenessEntity() {
    }

    public CompletenessEntity(final EntityType entityType, final String entityId) {
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @Generated
    public EntityType getEntityType() {
        return entityType;
    }

    @Generated
    public String getEntityId() {
        return entityId;
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

    public static class CompletenessId implements Serializable {
        private EntityType entityType;
        private String entityId;

        public CompletenessId() {
        }

        public CompletenessId(final EntityType entityType, final String entityId) {
            this.entityType = entityType;
            this.entityId = entityId;
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompletenessId)) {
                return false;
            }
            final CompletenessId that = (CompletenessId) o;
            return entityType == that.entityType &&
                    Objects.equals(entityId, that.entityId);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(entityType, entityId);
        }
    }
}
