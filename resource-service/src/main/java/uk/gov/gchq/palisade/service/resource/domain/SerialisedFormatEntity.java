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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.StringJoiner;

@Entity
@Table(name = "serialised_formats",
        indexes = {
                @Index(name = "serialised_format", columnList = "serialised_format"),
        })
@RedisHash("SerialisedFormatEntity")
public class SerialisedFormatEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @org.springframework.data.annotation.Id
    @Column(name = "resource_id", columnDefinition = "varchar(255)", nullable = false)
    private String resourceId;

    @Indexed
    @Column(name = "serialised_format", columnDefinition = "varchar(255)", nullable = false)
    private String serialisedFormat;

    public SerialisedFormatEntity() {
    }

    @PersistenceConstructor
    public SerialisedFormatEntity(final String serialisedFormat, final String resourceId) {
        this.serialisedFormat = serialisedFormat;
        this.resourceId = resourceId;
    }

    @Generated
    public String getSerialisedFormat() {
        return serialisedFormat;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", SerialisedFormatEntity.class.getSimpleName() + "[", "]")
                .add("resourceId='" + resourceId + "'")
                .add("serialisedFormat='" + serialisedFormat + "'")
                .add(super.toString())
                .toString();
    }
}
