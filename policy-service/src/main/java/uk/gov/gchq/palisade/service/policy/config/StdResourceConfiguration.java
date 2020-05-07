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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.ResourcePrepopulationFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class StdResourceConfiguration implements ResourceConfiguration {
    private List<StdResourcePrepopulationFactory> resources;

    /**
     * Constructor with 0 arguments for a standard implementation
     * of the {@link ResourceConfiguration} interface
     */
    public StdResourceConfiguration() {
        resources = Collections.emptyList();
    }

    /**
     * Constructor with 1 argument for a standard implementation
     * of the {@link ResourceConfiguration} interface
     *
     * @param resources     a list of objects implementing the {@link ResourcePrepopulationFactory} interface
     */
    public StdResourceConfiguration(final List<StdResourcePrepopulationFactory> resources) {
        this.resources = resources;
    }

    @Override
    @Generated
    public List<StdResourcePrepopulationFactory> getResources() {
        return resources;
    }

    @Generated
    public void setResources(final List<StdResourcePrepopulationFactory> resources) {
        requireNonNull(resources);
        this.resources = resources;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdResourceConfiguration)) {
            return false;
        }
        final StdResourceConfiguration that = (StdResourceConfiguration) o;
        return Objects.equals(resources, that.resources);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(resources);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdResourceConfiguration.class.getSimpleName() + "[", "]")
                .add("resources=" + resources)
                .add(super.toString())
                .toString();
    }
}
