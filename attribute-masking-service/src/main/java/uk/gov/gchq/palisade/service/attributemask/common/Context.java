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

package uk.gov.gchq.palisade.service.attributemask.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
@JsonPropertyOrder(value = {"class", "contents"}, alphabetic = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "class"
)
public class Context {

    private static final String PURPOSE = "purpose";
    private Map<String, Object> contents;

    public Context() {
        this(new HashMap<>());
    }

    @JsonCreator
    public Context(@JsonProperty("contents") final Map<String, Object> contents) {
        this.setContents(contents);
    }

    @Generated
    public Context contents(final Map<String, Object> contents) {
        this.setContents(contents);
        return this;
    }

    @Generated
    public Map<String, Object> getContents() {
        return contents;
    }

    @Generated
    public void setContents(final Map<String, Object> contents) {
        requireNonNull(contents);
        this.contents = contents;
    }

    @JsonIgnore
    @Generated
    public Map<String, Object> getContentsCopy() {
        return Collections.unmodifiableMap(contents);
    }

    @JsonIgnore
    @Generated
    public Context purpose(final String purpose) {
        contents.put(PURPOSE, purpose);
        return this;
    }

    @JsonIgnore
    public String getPurpose() {
        try {
            return (String) contents.get(PURPOSE);
        } catch (final ClassCastException e) {
            throw new RuntimeException("The purpose value should be a string");
        }
    }

    @Generated
    public Object get(final String key) {
        return contents.get(key);
    }

    @Generated
    public Context put(final String key, final Object value) {
        requireNonNull(key, "The key cannot be null.");
        requireNonNull(value, "The value cannot be null.");
        contents.put(key, value);
        return this;
    }

    @Generated
    public Context putIfAbsent(final String key, final Object value) {
        requireNonNull(key, "The key cannot be null.");
        requireNonNull(value, "The value cannot be null.");
        contents.putIfAbsent(key, value);
        return this;
    }

    @JsonGetter("class")
    @Generated
    public String getClassName() {
        return getClass().getName();
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
