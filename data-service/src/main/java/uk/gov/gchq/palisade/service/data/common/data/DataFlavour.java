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

package uk.gov.gchq.palisade.service.data.common.data;

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * In Palisade, to process a resource, e.g. to de-serialise it, we must know its data type (what type of object the resource
 * contains, e.g. employee records) and its serialised format (how it is stored, e.g. Avro). A {@code DataFlavour} is the combination
 * of these two features.
 */
public class DataFlavour {
    /**
     * The internal store of the flavour. The left entry is the data type and the right entry is the serialised format.
     */
    private final String type;
    private final String serialisedFormat;

    /**
     * Create a flavour.
     *
     * @param dataType         the type of record or object being described
     * @param serialisedFormat the encoding format for storing the data
     * @throws IllegalArgumentException if either parameter is empty or blank
     */
    public DataFlavour(final String dataType, final String serialisedFormat) {
        requireNonNull(dataType, "dataType");
        requireNonNull(serialisedFormat, "serialisedFormat");
        if (dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("dataType cannot be empty");
        }
        if (serialisedFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("serialisedFormat cannot be empty");
        }
        this.type = dataType;
        this.serialisedFormat = serialisedFormat;
    }

    /**
     * Convenience method to create a {@link DataFlavour}.
     *
     * @param dataType         the data type
     * @param serialisedFormat the serialised format
     * @return a new {@link DataFlavour}
     * @throws IllegalArgumentException if either parameter is empty or blank
     */
    @Generated
    public static DataFlavour of(final String dataType, final String serialisedFormat) {
        return new DataFlavour(dataType, serialisedFormat);
    }

    /**
     * The data type. This is the type of entity that is being described by this flavour, e.g. employee record or bank account record
     *
     * @return data type
     */
    @Generated
    public String getDataType() {
        return this.type;
    }

    /**
     * The serialised format. This is the method of storing the data type in a machine processable format, e.g. Avro or JSON.
     *
     * @return serialised format
     */
    @Generated
    public String getSerialisedFormat() {
        return this.serialisedFormat;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataFlavour)) {
            return false;
        }
        final DataFlavour that = (DataFlavour) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(serialisedFormat, that.serialisedFormat);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(type, serialisedFormat);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", DataFlavour.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("serialisedFormat=" + serialisedFormat)
                .toString();
    }
}
