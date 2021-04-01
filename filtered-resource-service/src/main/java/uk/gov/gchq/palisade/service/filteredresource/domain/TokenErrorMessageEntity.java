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
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;

import javax.persistence.Column;
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
        name = "token_error",
        indexes = {
                @Index(name = "token", columnList = "token")
        }
)
@RedisHash(value = "TokenErrorMessageEntity", timeToLive = 6000L)
public class TokenErrorMessageEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "token", columnDefinition = "varchar(255)")
    @Indexed
    protected String token;

    @Column(name = "service_name", columnDefinition = "varchar(255)")
    private String serviceName;

    @Column(name = "error", columnDefinition = "varchar(255)")
    private String error;

    @TimeToLive
    protected Long timeToLive;

    public TokenErrorMessageEntity() {
        // no-args constructor
    }

    /**
     * Constructor used for the Database that takes a {@link String} and Exception
     * Used for inserting objects into the backing store
     *
     * @param token       the token {@link String} for the client request as a whole, created by the Palisade-Service
     * @param serviceName the name of the service that originally threw the error
     * @param error       the error attached to the {@link AuditErrorMessage}
     */
    @PersistenceConstructor
    public TokenErrorMessageEntity(final String token, final String serviceName, final String error) {
        this.token = token;
        this.serviceName = serviceName;
        this.error = error;
        this.timeToLive = RedisTtlConfiguration.getTimeToLiveSeconds("TokenErrorMessageEntity");
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getServiceName() {
        return serviceName;
    }

    @Generated
    public String getError() {
        return error;
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
        final TokenErrorMessageEntity that = (TokenErrorMessageEntity) o;
        return Objects.equals(token, that.token) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(error, that.error);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, serviceName, error);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TokenErrorMessageEntity.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("serviceName=" + serviceName)
                .add("error=" + error)
                .add(super.toString())
                .toString();
    }
}
