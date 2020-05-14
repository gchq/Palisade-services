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

package uk.gov.gchq.palisade.service.data.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;

import java.lang.reflect.Constructor;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StdSerialiserPrepopulationFactory} that uses Spring to configure a resource from a yaml file
 * A factory for {@link Serialiser} objects, using:
 * - a {@link Map} of the type and format required for a {@link uk.gov.gchq.palisade.reader.common.DataFlavour}
 * - a {@link Map} of the serialiser class and the domain class needed to create a {@link Serialiser}
 */
public class StdSerialiserPrepopulationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdSerialiserPrepopulationFactory.class);

    private Map<String, String> dataFlavour;
    private Map<String, String> serialiserMap;

    /**
     * Constructor with 0 arguments for a {@link StdSerialiserPrepopulationFactory} object
     */
    public StdSerialiserPrepopulationFactory() {
        dataFlavour = Collections.emptyMap();
        serialiserMap = Collections.emptyMap();
    }

    /**
     * Creates a {@link StdSerialiserPrepopulationFactory}, passing each member as an argument
     *
     * @param dataFlavour   A {@link Map} containing a {@link String} value of the data type and a {@link String} value of the data format
     *                      for a {@link uk.gov.gchq.palisade.reader.common.DataFlavour}
     * @param serialiser    A {@link Map} containing a {@link String} value opf the serialiser class and a {@link String} value of the domain
     *                      class for a {@link Serialiser}
     */
    public StdSerialiserPrepopulationFactory(final Map<String, String> dataFlavour,
                                             final Map<String, String> serialiser) {
        this.dataFlavour = dataFlavour;
        this.serialiserMap = serialiser;
    }

    @Generated
    public Map<String, String> getDataFlavour() {
        return dataFlavour;
    }

    @Generated
    public void setDataFlavour(final Map<String, String> dataFlavour) {
        requireNonNull(dataFlavour);
        this.dataFlavour = dataFlavour;
    }

    @Generated
    public Map<String, String> getSerialiser() {
        return serialiserMap;
    }

    @Generated
    public void setSerialiser(final Map<String, String> serialiser) {
        requireNonNull(serialiser);
        this.serialiserMap = serialiser;
    }

    public Entry<DataFlavour, Serialiser<?>> build() {
        DataFlavour flavour = null;
        Serialiser<?> serialiser = null;
        for (Entry<String, String> entry : dataFlavour.entrySet()) {
            flavour = DataFlavour.of(entry.getKey(), entry.getValue());
        }
        for (Entry<String, String> entry : serialiserMap.entrySet()) {
            try {
                serialiser = (Serialiser<?>) Class.forName(entry.getKey())
                        .getConstructor(Class.class)
                        .newInstance(Class.forName(entry.getValue()));
            } catch (Exception ex) {
                LOGGER.error("Error getting the serialiser {} class and the domain {} class: {}", entry.getKey(), entry.getValue(), ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
        return new SimpleImmutableEntry<>(flavour, serialiser);
    }
}
