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
package uk.gov.gchq.palisade.service.attributemask.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Generated;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.user.User;
import uk.gov.gchq.palisade.service.attributemask.config.RedisTtlConfiguration;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * An entity for access to a single leafResource with a set of rules (and user/context to apply)
 * to be persisted in a repository/database. A (unique) key is created from the concatenation of
 * the token and leafResource id, which is used for indexing. This will later be retrieved by the
 * data-service to assert the client's access has been authorised, and the rules for such access.
 */
@Entity
@Table(
        name = "authorised_requests",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "unique_id"),
                @UniqueConstraint(columnNames = {"token", "resource_id"})
        }
)
@RedisHash(value = "AuthorisedRequestEntity", timeToLive = 86400)
public class AuthorisedRequestEntity {

    @Id
    @Column(name = "unique_id", columnDefinition = "varchar(255)")
    @org.springframework.data.annotation.Id
    @Indexed
    private String uniqueId;

    @Column(name = "token", columnDefinition = "varchar(255)")
    private String token;

    @Column(name = "resource_id", columnDefinition = "varchar(255)")
    private String resourceId;

    @Column(name = "user", columnDefinition = "clob")
    @Convert(converter = UserConverter.class)
    private User user;

    @Column(name = "leaf_resource", columnDefinition = "clob")
    @Convert(converter = LeafResourceConverter.class)
    private LeafResource leafResource;

    @Column(name = "context", columnDefinition = "clob")
    @Convert(converter = ContextConverter.class)
    private Context context;

    @Column(name = "rules", columnDefinition = "clob")
    @Convert(converter = RulesConverter.class)
    private Rules<?> rules;

    @TimeToLive
    protected Long timeToLive;

    /**
     * Empty-constructor for deserialisation functions
     */
    public AuthorisedRequestEntity() {
        // Empty constructor
    }

    /**
     * Constructor for an AuthorisedRequestEntity to be persisted in a repository/database.
     * A (unique) key is created from the concatenation of the token and leafResource id, which is used for indexing
     *
     * @param token        the token {@link String} for the client request as a whole, created by the palisade-service
     * @param user         the {@link User} as authorised and returned by the user-service
     * @param leafResource one of many {@link LeafResource} as discovered and returned by the resource-service
     * @param context      the {@link Context} as originally supplied by the client
     * @param rules        the {@link Rules} that will be applied to the resource and its records as returned by the policy-service
     */
    @PersistenceConstructor
    public AuthorisedRequestEntity(final String token, final User user, final LeafResource leafResource, final Context context, final Rules<?> rules) {
        this.uniqueId = new AuthorisedRequestEntityId(token, leafResource.getId()).getUniqueId();
        this.token = token;
        this.resourceId = leafResource.getId();
        this.user = user;
        this.leafResource = leafResource;
        this.context = context;
        this.rules = rules;
        this.timeToLive = RedisTtlConfiguration.getTimeToLiveSeconds("AuthorisedRequestEntity");
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public LeafResource getLeafResource() {
        return leafResource;
    }

    @Generated
    public Context getContext() {
        return context;
    }

    @Generated
    public Rules<?> getRules() {
        return rules;
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
        if (!(o instanceof AuthorisedRequestEntity)) {
            return false;
        }
        final AuthorisedRequestEntity that = (AuthorisedRequestEntity) o;
        return Objects.equals(uniqueId, that.uniqueId) &&
                Objects.equals(token, that.token) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(user, that.user) &&
                Objects.equals(leafResource, that.leafResource) &&
                Objects.equals(context, that.context) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(uniqueId, token, resourceId, user, leafResource, context, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuthorisedRequestEntity.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("resourceId='" + resourceId + "'")
                .add("user=" + user)
                .add("leafResource=" + leafResource)
                .add("context=" + context)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }

    /**
     * Helper class for mapping tokens and resourceIds to a (unique) product of the two
     */
    public static class AuthorisedRequestEntityId {
        private final String token;
        private final String resourceId;

        /**
         * Basic constructor taking in the pair of non-unique keys
         *
         * @param token      the token of the request - unique per each new client request
         * @param resourceId the resource id for this response - unique per resource and
         *                   thus across all returned resources for this request
         */
        public AuthorisedRequestEntityId(final String token, final String resourceId) {
            this.token = token;
            this.resourceId = resourceId;
        }

        /**
         * Create a unique product of the token and resourceId
         * Concatenate the two strings with a separator that shouldn't appear in either String
         *
         * @return a unique id for indexing the entity on
         */
        public String getUniqueId() {
            return token + "::" + resourceId;
        }
    }
}
