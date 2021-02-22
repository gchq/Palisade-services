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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.filteredresource.config.RedisTtlConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Database entity storing a token and its exception from the Error Topic
 */
@Entity
@Table(
        name = "token_exception",
        indexes = {
                @Index(name = "token", columnList = "token")
        }
)
@RedisHash(value = "TokenAuditErrorMessageEntity", timeToLive = 6000L)
public class TokenAuditErrorMessageEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "token", columnDefinition = "varchar(255)")
    @Indexed
    protected String token;

    @Column(name = "message", columnDefinition = "clob")
    @Convert(converter = AuditErrorMessageConverter.class)
    private AuditErrorMessage auditErrorMessage;

    @TimeToLive
    protected Long timeToLive;

    /**
     * Empty-constructor for (de)serialisation functions
     */
    public TokenAuditErrorMessageEntity() {
        // no-args constructor
    }

    /**
     * Constructor taking a token and AuditErrorMessage
     *
     * @param token             the unique request token
     * @param auditErrorMessage the {@link AuditErrorMessage} used to populate this class.
     */
    @PersistenceConstructor
    public TokenAuditErrorMessageEntity(final String token, final AuditErrorMessage auditErrorMessage) {
        this.token = token;
        this.auditErrorMessage = auditErrorMessage;
        this.timeToLive = RedisTtlConfiguration.getTimeToLiveSeconds("TokenAuditErrorMessageEntity");
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TokenAuditErrorMessageEntity that = (TokenAuditErrorMessageEntity) o;
        return Objects.equals(token, that.token) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TokenAuditErrorMessageEntity.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("auditErrorMessage=" + auditErrorMessage)
                .add(super.toString())
                .toString();
    }
}
