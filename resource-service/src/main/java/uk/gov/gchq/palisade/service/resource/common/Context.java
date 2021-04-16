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

package uk.gov.gchq.palisade.service.resource.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A structure to store contextual information from the client at query time that can be used when interacting with resources.
 * Interaction with a resource include both reading and writing and will often require
 * additional information that can be stored and recovered in this structure and passed along with the request/operation.
 * i.e. A users purpose for requesting the contents of a file.
 */
@JsonTypeInfo(use = Id.NAME)
public class Context {

    private static final String PURPOSE = "purpose";
    private Map<String, Object> contents;

    /**
     * Create a new context object with an empty hashMap of attributes
     */
    public Context() {
        this(new HashMap<>());
    }

    /**
     * Create a new context object, passing in a map of attributes
     *
     * @param contents a map of attributes, containing a purpose
     */
    @JsonCreator
    @SuppressWarnings("java:S1699")
    public Context(@JsonProperty("contents") final Map<String, Object> contents) {
        this.setContents(contents);
    }

    /**
     * Sets the contents of this Context object
     *
     * @param contents a map of contents that will be added to this class
     * @return the {@link Context} object
     */
    @Generated
    public Context contents(final Map<String, Object> contents) {
        this.setContents(contents);
        return this;
    }

    /**
     * Get the contents map of the {@link Context}
     *
     * @return a map of {@link String} and {@link Object}
     */
    @Generated
    public Map<String, Object> getContents() {
        return contents;
    }

    /**
     * Set the contents map of the {@link Context}
     *
     * @param contents the map to be added to the Context
     */
    @Generated
    public void setContents(final Map<String, Object> contents) {
        requireNonNull(contents);
        this.contents = contents;
    }

    /**
     * Get a copy of the contents map of the {@link Context}
     *
     * @return an unmodifiable map of the contents
     */
    @JsonIgnore
    @Generated
    public Map<String, Object> getContentsCopy() {
        return Collections.unmodifiableMap(contents);
    }

    /**
     * Adds a purpose, or reason for requesting data to the Context object.
     *
     * @param purpose a String containing why the User wants access to the data
     * @return the Context object with the purpose added to the contents map
     */
    @JsonIgnore
    @Generated
    public Context purpose(final String purpose) {
        contents.put(PURPOSE, purpose);
        return this;
    }

    /**
     * Get the purpose from the contents map of the {@link Context}
     *
     * @return a string value of the purpose
     */
    @SuppressWarnings({"java:S112", "java:S1166"})
    @JsonIgnore
    public String getPurpose() {
        try {
            return (String) contents.get(PURPOSE);
        } catch (final ClassCastException e) {
            throw new RuntimeException("The purpose value should be a string");
        }
    }

    /**
     * Get the purpose by the key value in the map
     *
     * @param key the key value associated with the purpose
     * @return the Object purpose associated with to the String key
     */
    @Generated
    public Object get(final String key) {
        return contents.get(key);
    }

    /**
     * Put the provided key and value into the contents map
     *
     * @param key   the key value
     * @param value the value object
     * @return the {@link Context} object
     */
    @Generated
    public Context put(final String key, final Object value) {
        requireNonNull(key, "The key cannot be null.");
        requireNonNull(value, "The value cannot be null.");
        contents.put(key, value);
        return this;
    }

    /**
     * Put the provided key and value into the contents map if it does not already exist
     *
     * @param key   the key value
     * @param value the value object
     * @return the {@link Context} object
     */
    @Generated
    public Context putIfAbsent(final String key, final Object value) {
        requireNonNull(key, "The key cannot be null.");
        requireNonNull(value, "The value cannot be null.");
        contents.putIfAbsent(key, value);
        return this;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Context)) {
            return false;
        }
        Context context = (Context) o;
        return contents.equals(context.contents);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(contents);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", Context.class.getSimpleName() + "[", "]")
                .add("contents=" + contents)
                .toString();
    }
}
