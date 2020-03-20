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

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.CacheWarmerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties
public class StdUserCacheWarmerFactory implements CacheWarmerFactory {

    private String userId;
    private Set<String> auths;
    private Set<String> roles;

    public StdUserCacheWarmerFactory() {
        this.userId = "";
        this.auths = Collections.emptySet();
        this.roles = Collections.emptySet();
    }

    public StdUserCacheWarmerFactory(final String userId, final Set<String> roles, final Set<String> auths) {
        this.userId = userId;
        this.auths = auths;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public Set<String> getAuths() {
        return auths;
    }

    public StdUserCacheWarmerFactory setAuths(final Set<String> auths) {
        requireNonNull(auths, "Cannot add null auths.");
        this.auths = auths;
        return this;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public StdUserCacheWarmerFactory setRoles(final Set<String> roles) {
        requireNonNull(roles, "Cannot add null roles.");
        this.roles = roles;
        return this;
    }

    @Override
    public User warm() {
        System.out.println("Using the StdUser warm method");
        return new User()
                .userId(this.getUserId())
                .roles(this.getRoles())
                .auths(this.getAuths());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StdUserCacheWarmerFactory that = (StdUserCacheWarmerFactory) o;

        return Objects.equals(userId, that.userId) &&
                Objects.equals(roles, that.roles) &&
                Objects.equals(auths, that.auths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roles, auths);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Users{\n");
        sb.append("\tuserId=").append(userId).append('\n');
        sb.append("\tauths=").append(auths).append('\n');
        sb.append("\troles=").append(roles).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
