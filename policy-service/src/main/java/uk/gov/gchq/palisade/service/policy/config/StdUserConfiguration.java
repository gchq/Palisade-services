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

package uk.gov.gchq.palisade.service.policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.UserConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "population")
public class StdUserConfiguration implements UserConfiguration {

    private List<StdUserCacheWarmerFactory> users = new ArrayList<>();

    /**
     * Constructor with 0 arguments for a standard implementation
     * of the {@link UserConfiguration} interface
     */
    public StdUserConfiguration() {
    }

    /**
     * Constructor with 1 argument for a standard implementation
     * of the {@link UserConfiguration} interface
     *
     * @param users     a list of objects implementing the {@link uk.gov.gchq.palisade.service.UserCacheWarmerFactory} interface
     */
    public StdUserConfiguration(final List<StdUserCacheWarmerFactory> users) {
        this.setUsers(users);
    }

    @Override
    @Generated
    public List<StdUserCacheWarmerFactory> getUsers() {
        return users;
    }

    @Generated
    public void setUsers(final List<StdUserCacheWarmerFactory> users) {
        requireNonNull(users);
        this.users = users;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdUserConfiguration)) {
            return false;
        }
        final StdUserConfiguration that = (StdUserConfiguration) o;
        return Objects.equals(users, that.users);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(users);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdUserConfiguration.class.getSimpleName() + "[", "]")
                .add("users=" + users)
                .add(super.toString())
                .toString();
    }
}
