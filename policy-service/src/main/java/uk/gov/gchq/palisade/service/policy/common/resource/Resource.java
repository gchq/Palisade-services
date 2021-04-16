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

package uk.gov.gchq.palisade.service.policy.common.resource;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.io.Serializable;

/**
 * A high level API to define a resource, where a resource could be a system, directory, file, stream, etc.
 * A resource is expected to have a unique identifier.
 */
@JsonTypeInfo(use = Id.NAME)
public interface Resource extends Comparable<Resource>, Serializable {

    /**
     * Sets the id of the resource
     *
     * @param id a String value of the resources Id
     * @return the id of the newly created resource
     */
    Resource id(String id);

    String getId();

    void setId(final String id);

}
