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

import uk.gov.gchq.palisade.service.UserConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "population")
public class StdUserConfiguration implements UserConfiguration {

    private List<StdUserCacheWarmerFactory> users = new ArrayList<>();

    public StdUserConfiguration() {
    }

    public StdUserConfiguration(final List<StdUserCacheWarmerFactory> users) {
        this.users = users;
    }

    @Override
    public List<StdUserCacheWarmerFactory> getUsers() {
        return users;
    }

    public void setUsers(final List<StdUserCacheWarmerFactory> users) {
        this.users = users;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StdUserConfiguration that = (StdUserConfiguration) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }

    @Override
    public String toString() {
        return "StdUserConfiguration{" +
                "users=" + users +
                '}';
    }
}
