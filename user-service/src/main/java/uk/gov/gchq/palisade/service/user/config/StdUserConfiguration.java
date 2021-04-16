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
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.common.user.UserConfiguration;
import uk.gov.gchq.palisade.service.user.common.user.UserPrepopulationFactory;
import uk.gov.gchq.palisade.service.user.common.user.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a {@link UserConfiguration} that uses Spring to configure a list of users from a yaml file
 * A container for a number of {@link StdUserPrepopulationFactory} builders used for creating {@link User}s
 * These users will be used for pre-populating the {@link UserService}
 */
public class StdUserConfiguration implements UserConfiguration {
    private List<StdUserPrepopulationFactory> users;

    /**
     * Constructor with 0 arguments for a standard implementation
     * of the {@link UserConfiguration} interface
     */
    public StdUserConfiguration() {
        this.users = Collections.emptyList();
    }

    /**
     * Constructor with 1 argument for a standard implementation
     * of the {@link UserConfiguration} interface
     *
     * @param users a list of objects implementing the {@link UserPrepopulationFactory} interface
     */
    public StdUserConfiguration(final List<StdUserPrepopulationFactory> users) {
        this.users = new ArrayList<>(users);
    }

    @Override
    @Generated
    public List<StdUserPrepopulationFactory> getUsers() {
        return new ArrayList<>(users);
    }

    @Generated
    public void setUsers(final List<StdUserPrepopulationFactory> users) {
        requireNonNull(users);
        this.users = new ArrayList<>(users);
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
