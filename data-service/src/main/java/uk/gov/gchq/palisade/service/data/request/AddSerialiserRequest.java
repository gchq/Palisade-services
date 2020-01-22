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

package uk.gov.gchq.palisade.service.data.request;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to send a request to the
 * {@link uk.gov.gchq.palisade.service.data.service.DataService} to add a serialiser to the cache.
 */
public class AddSerialiserRequest extends Request {

    private DataFlavour dataFlavour;
    private Serialiser<?> serialiser;

    public AddSerialiserRequest dataFlavour(final DataFlavour dataFlavour) {
        requireNonNull(dataFlavour, "The data flavour cannot be set to null.");
        this.dataFlavour = dataFlavour;
        return this;
    }

    public AddSerialiserRequest serialiser(final Serialiser<?> serialiser) {
        requireNonNull(serialiser, "The serialiser cannot be set to null.");
        this.serialiser = serialiser;
        return this;
    }

    public DataFlavour getDataFlavour() {
        requireNonNull(dataFlavour, "The data flavour has not been set.");
        return dataFlavour;
    }

    public void setDataFlavour(final DataFlavour dataFlavour) {
        dataFlavour(dataFlavour);
    }

    public Serialiser<?> getSerialiser() {
        requireNonNull(serialiser, "The serialiser has not been set.");
        return serialiser;
    }

    public void setSerialiser(final Serialiser<?> serialiser) {
        serialiser(serialiser);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AddSerialiserRequest that = (AddSerialiserRequest) o;
        return dataFlavour.equals(that.dataFlavour) &&
                serialiser.equals(that.serialiser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataFlavour, serialiser);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("dataFlavour", dataFlavour)
                .append("serialiser", serialiser)
                .toString();
    }
}
