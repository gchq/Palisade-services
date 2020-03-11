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

package uk.gov.gchq.palisade.service.user.config;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class UserConfiguration {

    private Set<String> roles = Collections.emptySet();
    private Set<String> auths = Collections.emptySet();

    public UserConfiguration auths(final String... auths) {
        requireNonNull(auths, "Cannot add null auths.");
        this.auths.clear();
        Collections.addAll(this.auths, auths);
        return this;
    }

    public UserConfiguration auths(final Set<String> auths) {
        requireNonNull(auths, "Cannot add null auths.");
        this.auths.clear();
        this.auths.addAll(auths);
        return this;
    }

    public void setAuths(final Set<String> auths) {
        auths(auths);
    }

    public Set<String> getAuths() {
        return auths;
    }

    public UserConfiguration addAuths(final Set<String> auths) {
        requireNonNull(auths, "Cannot add null auths.");
        this.auths.addAll(auths);
        return this;
    }

    public UserConfiguration roles(final String... roles) {
        requireNonNull(roles, "Cannot add null roles.");
        this.roles.clear();
        Collections.addAll(this.roles, roles);
        return this;
    }

    public void setRoles(final Set<String> roles) {
        roles(roles);
    }

    public UserConfiguration roles(final Set<String> roles) {
        requireNonNull(roles, "Cannot add null roles.");
        this.roles.clear();
        this.roles.addAll(roles);
        return this;
    }

    public Set<String> getRoles() {
        // roles cannot be null
        return roles;
    }

    public UserConfiguration addRoles(final Set<String> roles) {
        requireNonNull(roles, "Cannot add null roles.");
        this.roles.addAll(roles);
        return this;
    }

    public User buildUser(final UserId userId) {
        return new User()
                .userId(userId)
                .addAuths(this.getAuths())
                .roles(this.getRoles());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UserConfiguration that = (UserConfiguration) o;

        return Objects.equals(roles, that.roles) &&
                Objects.equals(auths, that.auths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roles, auths);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserConfiguration{\n");
        sb.append("\troles=").append(roles).append('\n');
        sb.append("\tauths=").append(auths).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
