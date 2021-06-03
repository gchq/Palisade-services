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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.exception.SerialiserConstructorNotFoundException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserInitialisationException;
import uk.gov.gchq.palisade.service.data.exception.SerialiserNotFoundException;
import uk.gov.gchq.palisade.service.data.reader.DataFlavour;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file.
 * A factory for {@link Serialiser} objects, using:
 * - A {@link String} value of the serialised format of a file
 * - A {@link String} value of the fully qualified class that will be the file type
 * - A {@link String} value of the fully qualified class of the serialiser that will be created.
 */
public class StdSerialiserPrepopulationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdSerialiserPrepopulationFactory.class);

    private String flavourFormat = "";
    private String flavourType = "";
    private String serialiserClass = "";

    /**
     * Constructor with 0 arguments for a {@link StdSerialiserPrepopulationFactory} object.
     */
    public StdSerialiserPrepopulationFactory() {
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
     * Creates a {@link DataFlavour} and a {@link Serialiser} using the values within a {@link StdSerialiserPrepopulationFactory}.
     *
     * @return an {@link Entry} that consists of the created {@link DataFlavour} and {@link Serialiser} objects.
     */
    @SuppressWarnings("unchecked")
    public Entry<DataFlavour, Serialiser<Object>> build() {
        Serialiser<Object> serialiser;
        LOGGER.info("Building serialiser for class '{}', resource format '{}' and domain type '{}'", serialiserClass, flavourFormat, flavourType);
        try {
            Class<?> serialiserClazz = Class.forName(serialiserClass);
            LOGGER.debug("Got class {} for serialiser classname {}", serialiserClazz, serialiserClass);
            Constructor<?> serialiserConstructor = serialiserClazz.getConstructor(Class.class);
            LOGGER.debug("Got one-element constructor {} for class {}", serialiserConstructor, serialiserClazz);
            Class<?> typeClass = Class.forName(flavourType);
            LOGGER.debug("Got class {} for flavourType classname {}", typeClass, flavourType);
            serialiser = (Serialiser<Object>) serialiserConstructor.newInstance(typeClass);
        } catch (ClassNotFoundException ex) {
            throw new SerialiserNotFoundException("Error getting the serialiser class", ex);
        } catch (NoSuchMethodException ex) {
            throw new SerialiserConstructorNotFoundException("Error getting the serialiser constructor method", ex);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            throw new SerialiserInitialisationException("Error initialising the serialiser", ex);
        }
        var serdeEntry = new SimpleImmutableEntry<>(DataFlavour.of(flavourType, flavourFormat), serialiser);
        LOGGER.debug("Created serialiser entry {}", serdeEntry);
        return serdeEntry;
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
