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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.exception.SerialiserNotFoundException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SerialiserConfiguration {
    private Map<String, String> serialisers;

    @SuppressWarnings("unchecked")
    private static Class<Serialiser<?>> getSerialiserClass(final String className) {
        try {
            return (Class<Serialiser<?>>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new SerialiserNotFoundException("Could not find class for serialiser " + className, e);
        }
    }

    @Generated
    public Map<String, String> getSerialisers() {
        return serialisers;
    }

    public Map<String, Class<Serialiser<?>>> getSerialiserClassMap() {
        return serialisers.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), getSerialiserClass(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Generated
    public void setSerialisers(final Map<String, String> serialisers) {
        this.serialisers = Optional.ofNullable(serialisers)
                .orElseThrow(() -> new IllegalArgumentException("serialisers cannot be null"));
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SerialiserConfiguration)) {
            return false;
        }
        final SerialiserConfiguration that = (SerialiserConfiguration) o;
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
        return new StringJoiner(", ", SerialiserConfiguration.class.getSimpleName() + "[", "]")
                .add("serialisers=" + serialisers)
                .toString();
    }
}
