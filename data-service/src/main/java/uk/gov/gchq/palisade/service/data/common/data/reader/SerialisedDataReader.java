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

package uk.gov.gchq.palisade.service.data.common.data.reader;

import uk.gov.gchq.palisade.service.data.common.Generated;
import uk.gov.gchq.palisade.service.data.common.data.DataFlavour;
import uk.gov.gchq.palisade.service.data.common.data.seralise.Serialiser;
import uk.gov.gchq.palisade.service.data.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.serialise.SimpleStringSerialiser;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * This class is an abstract implementation of the {@link DataReader} which uses
 * serialisers to serialise the data into the for that the rules need to be able
 * to apply those rules and then de-serialise to how the client is expecting the
 * data to be returned.
 * <p>
 * This class means that the only places where the structure of the data needs
 * to be known is in the serialisers, rules and client code. Therefore, you only
 * need to implement a DataService for
 * each data storage technology and data format combination, rather than also
 * having to add the data structure into the mix.
 * <p>
 * A serialiser is chosen based on a {@link DataFlavour} which is a combination of
 * data type and serialised format.
 */
public abstract class SerialisedDataReader implements DataReader {

    private Serialiser<?> defaultSerialiser = new SimpleStringSerialiser();

    /**
     * Map of the types and formats to the serialising object. The first element of the key is the data type, and the second element is the serialised format.
     */
    private Map<DataFlavour, Serialiser<?>> serialisers = new ConcurrentHashMap<>();

    /**
     * Set the serialiser
     *
     * @param serialisers a mapping of data type to serialisers {@link SerialisedDataReader}
     * @return the {@link SerialisedDataReader}
     */
    @Generated
    public SerialisedDataReader serialisers(final Map<DataFlavour, Serialiser<?>> serialisers) {
        requireNonNull(serialisers, "The serialisers cannot be set to null.");
        this.setSerialisers(serialisers);
        return this;
    }

    /**
     * Set the default serialiser.
     *
     * @param serialiser the {@link Serialiser} to be set as the default
     * @return the {@link SerialisedDataReader} object
     */
    @Generated
    public SerialisedDataReader defaultSerialiser(final Serialiser<?> serialiser) {
        requireNonNull(serialiser, "The default serialiser cannot be set to null.");
        this.setDefaultSerialiser(serialiser);
        return this;
    }


    /**
     * This read method uses the serialiser that matches the data type of the
     * resource to serialise the raw data and apply the rules to the data and
     * then deserialise it back to the raw format expected by the client.
     *
     * @param request          {@link DataReaderRequest} containing the resource to be
     *                         read, rules to be applied, the user requesting the data
     *                         and the purpose for accessing the data.
     * @param recordsProcessed a counter for the number of records being processed
     * @param recordsReturned  a counter for the number of records being returned
     * @return a {@link DataReaderResponse} containing the stream of data
     * read to be streamed back to the client
     */
    @Override
    public DataReaderResponse read(final DataReaderRequest request, final AtomicLong recordsProcessed, final AtomicLong recordsReturned) {
        requireNonNull(request, "The request cannot be null.");

        final Serialiser<Serializable> serialiser = getSerialiser(request.getResource());
        //set up the raw input stream from the data source
        final InputStream rawStream = readRaw(request.getResource());

        ResponseWriter serialisedWriter = new SerialisingResponseWriter<>(rawStream, serialiser, request, recordsProcessed, recordsReturned);

        //set response object to use the writer above
        return new DataReaderResponse().writer(serialisedWriter);
    }

    /**
     * This is the method that connects to the data and streams the raw data
     * into the {@link SerialisedDataReader}.
     *
     * @param resource the resource to be accessed
     * @return a stream of data in the format that the client expects the data to be in.
     */
    protected abstract InputStream readRaw(final LeafResource resource);

    /**
     * Gets the associated {@link Serialiser} from the serialiser {@link Map} for a specific {@link DataFlavour}.
     *
     * @param flavour the {@link DataFlavour} value to retrieve a {@link Serialiser}
     * @param <T>     the type of {@link Serialiser}
     * @return the associated {@link Serialiser}
     */
    @Generated
    public <T> Serialiser<T> getSerialiser(final DataFlavour flavour) {
        requireNonNull(flavour, "The flavour cannot be null.");
        Serialiser<?> serialiser = serialisers.get(flavour);

        if (null == serialiser) {
            serialiser = defaultSerialiser;
        }
        return (Serialiser<T>) serialiser;
    }

    /**
     * Gets the associated {@link Serialiser} from the {@link Map} for a {@link LeafResource}.
     *
     * @param resource the {@link LeafResource} used to get the {@link Serialiser}
     * @param <I>      the type of {@link Serialiser}
     * @return the associated {@link Serialiser}
     */
    @Generated
    public <I> Serialiser<I> getSerialiser(final LeafResource resource) {
        return getSerialiser(DataFlavour.of(resource.getType(), resource.getSerialisedFormat()));
    }

    public void addSerialiser(final DataFlavour flavour, final Serialiser<?> serialiser) {
        requireNonNull(flavour, "The flavour cannot be null.");
        requireNonNull(serialiser, "The serialiser cannot be null.");
        serialisers.put(flavour, serialiser);
    }

    /**
     * Adds all the serialiser mappings to the current map of serialisers.Any existing mappings for a given {@link DataFlavour}
     * are replaced.
     *
     * @param mergingSerialisers the new serialisers to merge
     */
    public void addAllSerialisers(final Map<DataFlavour, Serialiser<?>> mergingSerialisers) {
        requireNonNull(mergingSerialisers, "mergingSerialisers");
        serialisers.putAll(mergingSerialisers);
    }

    @Generated
    public void setSerialisers(final Map<DataFlavour, Serialiser<?>> serialisers) {
        requireNonNull(serialisers);
        this.serialisers = serialisers;
    }

    @Generated
    public void setDefaultSerialiser(final Serialiser<?> defaultSerialiser) {
        requireNonNull(defaultSerialiser);
        this.defaultSerialiser = defaultSerialiser;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SerialisedDataReader)) {
            return false;
        }
        final SerialisedDataReader that = (SerialisedDataReader) o;
        return Objects.equals(defaultSerialiser, that.defaultSerialiser) &&
                Objects.equals(serialisers, that.serialisers);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(defaultSerialiser, serialisers);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", SerialisedDataReader.class.getSimpleName() + "[", "]")
                .add("defaultSerialiser=" + defaultSerialiser)
                .add("serialisers=" + serialisers)
                .toString();
    }
}
