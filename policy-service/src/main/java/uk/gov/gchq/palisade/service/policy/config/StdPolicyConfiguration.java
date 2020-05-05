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
import uk.gov.gchq.palisade.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.PolicyPrepopulationFactory;
import uk.gov.gchq.palisade.service.UserPrepopulationFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "population")
public class StdPolicyConfiguration implements PolicyConfiguration {

    private List<StdPolicyPrepopulationFactory> policies = new ArrayList<>();
    private List<StdUserPrepopulationFactory> users = new ArrayList<>();

    /**
     * Constructor with 0 arguments for a standard implementation
     * of the {@link PolicyConfiguration} interface
     */
    public StdPolicyConfiguration() {
    }

    /**
     * Constructor with 2 arguments for a standard implementation
     * of the {@link PolicyConfiguration} interface
     *
     * @param policies  a {@link List} of objects implementing the {@link PolicyPrepopulationFactory} interface
     * @param users  a {@link List} of objects implementing the {@link UserPrepopulationFactory} interface
     */
    public StdPolicyConfiguration(final List<StdPolicyPrepopulationFactory> policies,
                                  final List<StdUserPrepopulationFactory> users) {
        this.policies = policies;
        this.users = users;
    }

    @Generated
    public List<StdPolicyPrepopulationFactory> getPolicies() {
        return policies;
    }

    @Generated
    public void setPolicies(final List<StdPolicyPrepopulationFactory> policies) {
        requireNonNull(policies);
        this.policies = policies;
    }

    @Generated
    public List<StdUserPrepopulationFactory> getUsers() {
        return users;
    }

    @Generated
    public void setUsers(final List<StdUserPrepopulationFactory> users) {
        requireNonNull(users);
        this.users = users;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdPolicyConfiguration)) {
            return false;
        }
        final StdPolicyConfiguration that = (StdPolicyConfiguration) o;
        return Objects.equals(policies, that.policies) &&
                Objects.equals(users, that.users);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(policies, users);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdPolicyConfiguration.class.getSimpleName() + "[", "]")
                .add("policies=" + policies)
                .add("users=" + users)
                .add(super.toString())
                .toString();
    }
}
