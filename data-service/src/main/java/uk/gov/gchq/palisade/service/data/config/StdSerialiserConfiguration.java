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

package uk.gov.gchq.palisade.service.data.config;

import uk.gov.gchq.palisade.service.data.common.Generated;
import uk.gov.gchq.palisade.service.data.common.data.DataService;
import uk.gov.gchq.palisade.service.data.common.data.seralise.Serialiser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StdSerialiserConfiguration} object that uses Spring to configure a list of serialisers from a yaml file.
 * A container for a number of {@link StdSerialiserPrepopulationFactory} builders used for creating
 * {@link Serialiser}.  These serialisers will be used for pre-populating the {@link DataService}.
 */
public class StdSerialiserConfiguration {

    private List<StdSerialiserPrepopulationFactory> serialisers = new ArrayList<>();

    /**
     * Constructor with 0 arguments for a StdSerialiserConfiguration object
     */
    public StdSerialiserConfiguration() {
    }

    /**
     * Constructor with 1 arguments for a StdSerialiserConfiguration object.
     *
     * @param serialisers a {@link List} of objects of the {@link StdSerialiserPrepopulationFactory} class
     */
    public StdSerialiserConfiguration(final List<StdSerialiserPrepopulationFactory> serialisers) {
        this.serialisers = Collections.unmodifiableList(serialisers);
    }

    @Generated
    public List<StdSerialiserPrepopulationFactory> getSerialisers() {
        return Collections.unmodifiableList(serialisers);
    }

    @Generated
    public void setSerialisers(final List<StdSerialiserPrepopulationFactory> serialisers) {
        requireNonNull(serialisers);
        this.serialisers = Collections.unmodifiableList(serialisers);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdSerialiserConfiguration)) {
            return false;
        }
        final StdSerialiserConfiguration that = (StdSerialiserConfiguration) o;
        return Objects.equals(serialisers, that.serialisers);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(serialisers);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdSerialiserConfiguration.class.getSimpleName() + "[", "]")
                .add("serialisers=" + serialisers)
                .add(super.toString())
                .toString();
    }
}
