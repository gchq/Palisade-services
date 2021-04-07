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

import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.reader.common.data.seralise.Serialiser;
import uk.gov.gchq.palisade.service.data.common.Generated;
import uk.gov.gchq.palisade.service.data.exception.SerialiserConstructorNotFoundException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserInitialisationException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserNotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file
 * A factory for {@link Serialiser} objects, using:
 * - A {@link String} value of the serialised format of a file
 * - A {@link String} value of the fully qualified class that will be the file type
 * - A {@link String} value of the fully qualified class of the serialiser that will be created.
 */
public class StdSerialiserPrepopulationFactory {

    private String flavourFormat;
    private String flavourType;
    private String serialiserClass;

    /**
     * Constructor with 0 arguments for a StdSerialiserPrepopulationFactory object
     */
    public StdSerialiserPrepopulationFactory() {
        flavourFormat = "";
        flavourType = "";
        serialiserClass = "";
    }

    /**
     * Creates a StdSerialiserPrepopulationFactory, passing each member as an argument
     *
     * @param flavourFormat   a {@link String} value of the serialised format for a {@link DataFlavour}
     * @param flavourType     a {@link String} value of the fully qualified type for a {@link DataFlavour}
     * @param serialiserClass a {@link String} value of the fully qualified class to create a {@link Serialiser}
     */
    public StdSerialiserPrepopulationFactory(final String flavourFormat, final String flavourType, final String serialiserClass) {
        this.flavourFormat = flavourFormat;
        this.flavourType = flavourType;
        this.serialiserClass = serialiserClass;
    }

    @Generated
    public String getFlavourFormat() {
        return flavourFormat;
    }

    @Generated
    public void setFlavourFormat(final String flavourFormat) {
        requireNonNull(flavourFormat);
        this.flavourFormat = flavourFormat;
    }

    @Generated
    public String getFlavourType() {
        return flavourType;
    }

    @Generated
    public void setFlavourType(final String flavourType) {
        requireNonNull(flavourType);
        this.flavourType = flavourType;
    }

    @Generated
    public String getSerialiserClass() {
        return serialiserClass;
    }

    @Generated
    public void setSerialiserClass(final String serialiserClass) {
        requireNonNull(serialiserClass);
        this.serialiserClass = serialiserClass;
    }

    /**
     * Creates a {@link DataFlavour} and a {@link Serialiser} using the values within a StdSerialiserPrepopulationFactory
     *
     * @return an {@link Entry} that consists of the created {@link DataFlavour} and {@link Serialiser} objects.
     */
    public Entry<DataFlavour, Serialiser<Object>> build() {
        Serialiser<Object> serialiser;
        try {
            serialiser = (Serialiser<Object>) Class.forName(serialiserClass)
                    .getConstructor(Class.class)
                    .newInstance(Class.forName(flavourType));
        } catch (ClassNotFoundException ex) {
            throw new SerialiserNotFoundException("Error getting the serialiser class", ex);
        } catch (NoSuchMethodException ex) {
            throw new SerialiserConstructorNotFoundException("Error getting the serialiser constructor method", ex);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            throw new SerialiserInitialisationException("Error initialising the serialiser", ex);
        }
        return new SimpleImmutableEntry<>(DataFlavour.of(flavourType, flavourFormat), serialiser);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StdSerialiserPrepopulationFactory)) {
            return false;
        }
        final StdSerialiserPrepopulationFactory that = (StdSerialiserPrepopulationFactory) o;
        return Objects.equals(flavourFormat, that.flavourFormat) &&
                Objects.equals(flavourType, that.flavourType) &&
                Objects.equals(serialiserClass, that.serialiserClass);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(flavourFormat, flavourType, serialiserClass);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", StdSerialiserPrepopulationFactory.class.getSimpleName() + "[", "]")
                .add("flavourFormat='" + flavourFormat + "'")
                .add("flavourType='" + flavourType + "'")
                .add("serialiser='" + serialiserClass + "'")
                .add(super.toString())
                .toString();
    }
}
