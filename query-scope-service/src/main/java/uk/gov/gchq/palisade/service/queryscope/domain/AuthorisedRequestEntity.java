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
package uk.gov.gchq.palisade.service.queryscope.domain;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.StringJoiner;

@Entity
@Table(
        name = "authorised_requests",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "uniqueId"),
                @UniqueConstraint(columnNames = {"token", "resource_id"})
        }
)
public class AuthorisedRequestEntity implements Serializable {

    @Id
    @Column(name = "uniqueId", columnDefinition = "varchar(255)")
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

    public AuthorisedRequestEntity() {
    }

    public AuthorisedRequestEntity(final String token, final User user, final LeafResource leafResource, final Context context, final Rules<?> rules) {
        this.uniqueId = new AuthorisedRequestEntityId(token, leafResource.getId()).getUniqueId();
        this.token = token;
        this.resourceId = leafResource.getId();
        this.user = user;
        this.leafResource = leafResource;
        this.context = context;
        this.rules = rules;
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

    public static class AuthorisedRequestEntityId {
        private final String token;
        private final String resourceId;

        public AuthorisedRequestEntityId(final String token, final String resourceId) {
            this.token = token;
            this.resourceId = resourceId;
        }

        public String getUniqueId() {
            return token + "::" + resourceId;
        }
    }
}
