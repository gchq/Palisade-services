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

package uk.gov.gchq.palisade.service.filteredresource.common.service;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import java.io.Serializable;

/**
 * A High level API for passing details of how to connect to a resource
 */
@JsonPropertyOrder(value = {"class", "host", "port"}, alphabetic = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = As.EXISTING_PROPERTY,
        property = "class",
        defaultImpl = SimpleConnectionDetail.class
)
public interface ConnectionDetail extends Serializable {

    String createConnection();

    @JsonGetter("class")
    default String getClassName() {
        return getClass().getName();
    }

    @JsonSetter("class")
    default void setClassName(final String className) {
        // do nothing.
    }
}
