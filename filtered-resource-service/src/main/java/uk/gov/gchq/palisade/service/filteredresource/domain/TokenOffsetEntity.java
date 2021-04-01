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
package uk.gov.gchq.palisade.service.filteredresource.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import uk.gov.gchq.palisade.service.filteredresource.common.Generated;
import uk.gov.gchq.palisade.service.filteredresource.config.RedisTtlConfiguration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Database entity storing a token and its kafka commit offset
 */
@Entity
@Table(
        name = "token_offsets",
        indexes = {
                @Index(name = "message_token", columnList = "message_token", unique = true)
        }
)
@RedisHash(value = "TokenOffsetEntity", timeToLive = 6000L)
public class TokenOffsetEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "message_token", columnDefinition = "varchar(255)")
    @org.springframework.data.annotation.Id
    @Indexed
    protected String token;

    @Column(name = "commit_offset", columnDefinition = "long")
    protected Long offset;

    @TimeToLive
    protected Long timeToLive;

    public TokenOffsetEntity() {
        // no-args constructor
    }

    /**
     * Constructor used for the Database that takes a {@link String} and EntityId
     * Used for inserting objects into the backing store
     *
     * @param token  The Entity type enum object.
     * @param offset The Id of the entity, which eventually becomes a hash of the type and Id as a primary key
     */
    @PersistenceConstructor
    public TokenOffsetEntity(final String token, final Long offset) {
        this.token = token;
        this.offset = offset;
        this.timeToLive = RedisTtlConfiguration.getTimeToLiveSeconds("TokenOffsetEntity");
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public Long getOffset() {
        return offset;
    }

    @Generated
    public Long getTimeToLive() {
        return timeToLive;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenOffsetEntity)) {
            return false;
        }
        final TokenOffsetEntity that = (TokenOffsetEntity) o;
        return Objects.equals(token, that.token) &&
                Objects.equals(offset, that.offset);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, offset);
    }

}
