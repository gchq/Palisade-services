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

package uk.gov.gchq.palisade.service.user.config;

import uk.gov.gchq.palisade.service.user.common.Generated;
import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.common.service.UserPrepopulationFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a {@link UserPrepopulationFactory} that uses Spring to configure a user from a yaml file
 * A factory for {@link User} objects, using a userId, a list of authorizations and a list of roles.
 */
public class StdUserPrepopulationFactory implements UserPrepopulationFactory {

    private String userId;
    private Set<String> auths;
    private Set<String> roles;

    /**
     * Constructor with 0 arguments for a standard implementation
     * of the {@link UserPrepopulationFactory} interface.
     */
    public StdUserPrepopulationFactory() {
        this.userId = "";
        this.auths = Collections.emptySet();
        this.roles = Collections.emptySet();
    }

    /**
     * Constructor with 3 arguments for a standard implementation
     * of the {@link UserPrepopulationFactory} interface.
     *
     * @param userId a {@link String} value of a user.
     * @param roles  a {@link Set} of {@link String} role values for the user.
     * @param auths  a {@link Set} of {@link String} auth values for the user.
     */
    public StdUserPrepopulationFactory(final String userId, final Set<String> roles, final Set<String> auths) {
        this.userId = userId;
        this.auths = auths;
        this.roles = roles;
    }

    @Generated
    public String getUserId() {
        return userId;
    }

    @Generated
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    @Generated
    public Set<String> getAuths() {
        return auths;
    }

    @Generated
    public StdUserPrepopulationFactory setAuths(final Set<String> auths) {
        requireNonNull(auths, "Cannot add null auths.");
        this.auths = auths;
        return this;
    }

    @Generated
    public Set<String> getRoles() {
        return roles;
    }

    @Generated
    public StdUserPrepopulationFactory setRoles(final Set<String> roles) {
        requireNonNull(roles, "Cannot add null roles.");
        this.roles = roles;
        return this;
    }

    @Override
    public User build() {
        return new User()
                .userId(this.getUserId())
                .roles(this.getRoles())
                .auths(this.getAuths());
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdUserPrepopulationFactory)) {
            return false;
        }
        final StdUserPrepopulationFactory that = (StdUserPrepopulationFactory) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(auths, that.auths) &&
                Objects.equals(roles, that.roles);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, auths, roles);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdUserPrepopulationFactory.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("auths=" + auths)
                .add("roles=" + roles)
                .add(super.toString())
                .toString();
    }
}
