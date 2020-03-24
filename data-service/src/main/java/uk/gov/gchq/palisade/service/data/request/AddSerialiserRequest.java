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

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.service.data.Generated;
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

    @Generated
    public DataFlavour getDataFlavour() {
        return dataFlavour;
    }

    @Generated
    public void setDataFlavour(final DataFlavour dataFlavour) {
        this.dataFlavour = dataFlavour;
    }

    @Generated
    public Serialiser<?> getSerialiser() {
        return serialiser;
    }

    @Generated
    public void setSerialiser(final Serialiser<?> serialiser) {
        this.serialiser = serialiser;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AddSerialiserRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final AddSerialiserRequest that = (AddSerialiserRequest) o;
        return Objects.equals(dataFlavour, that.dataFlavour) &&
                Objects.equals(serialiser, that.serialiser);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataFlavour, serialiser);
    }

    @Override
    @Generated
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddSerialiserRequest{");
        sb.append("dataFlavour=").append(dataFlavour);
        sb.append(", serialiser=").append(serialiser);
        sb.append('}');
        return sb.toString();
    }
}
