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
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.util.StringJoiner;

@Entity
@Table(name = "types",
        indexes = {
                @Index(name = "type", columnList = "type"),
        })
public class TypeEntity {
    @Id
    @Column(name = "resource_id", columnDefinition = "varchar(255)", nullable = false)
    private String resourceId;

    @Column(name = "type", columnDefinition = "varchar(255)", nullable = false)
    private String type;

    public TypeEntity() {
    }

    public TypeEntity(final String type, final String resourceId) {
        this.type = type;
        this.resourceId = resourceId;
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
