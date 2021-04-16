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

package uk.gov.gchq.palisade.service.filteredresource.common.resource;

/**
 * A leaf resource is the interface for any resource that can be read for data
 * and is not just part of the hierarchical resource structure.
 * A LeafResource is expected to have a type and a serialised format. The type is a way of grouping
 * data of the same structure. The serialised format is the format of the file, e.g CSV, Parquet.
 */

public interface LeafResource extends ChildResource {

    /**
     * The type is a way of grouping data of the same structure.
     *
     * @param type a String value of the type
     * @return a LeafResource with a type attached
     */
    LeafResource type(final String type);

    /**
     * The serialised format is the format of the file, e.g CSV, Parquet.
     *
     * @param serialisedFormat a String value of the format
     * @return a LeafResource with a format attached
     */
    LeafResource serialisedFormat(final String serialisedFormat);

    /**
     * The connection detail is the location of the service for where the resource is stored, often as a serviceName
     *
     * @param connectionDetail a ConnectionDetail containing a serviceName or location of the resource
     * @return a LeafResource with a ConnectionDetail attached
     */
    LeafResource connectionDetail(final ConnectionDetail connectionDetail);

    String getType();

    String getSerialisedFormat();

    void setType(final String type);

    void setSerialisedFormat(final String serialisedFormat);

    ConnectionDetail getConnectionDetail();

    void setConnectionDetail(final ConnectionDetail connectionDetail);

}
