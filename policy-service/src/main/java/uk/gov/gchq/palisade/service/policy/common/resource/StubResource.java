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

import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;

import java.util.Comparator;

/**
 * A mock resource used in tests within this service
 */
public class StubResource extends AbstractLeafResource {
    private static final long serialVersionUID = 1L;

    private static final Comparator<StubResource> COMP = Comparator.comparing(StubResource::getSerialisedFormat)
            .thenComparing(StubResource::getType)
            .thenComparing(StubResource::getId);
    private static final SystemResource PARENT = new SystemResource().id("file");

    /**
     * Builder Constructor for the stub resource, taking in all individual componenets of a LeafResource and returning a StubResource
     *
     * @param type             the String value of the resources type
     * @param id               the id of the resource
     * @param format           the format of the resource, e.g. avro, txt
     * @param connectionDetail the name of the service which is storing the data
     */
    @SuppressWarnings("java:S1699")
    // supress constructor overridable method smell
    public StubResource(final String type, final String id, final String format, final ConnectionDetail connectionDetail) {
        this.id(id);
        this.type(type);
        this.serialisedFormat(format);
        this.connectionDetail(connectionDetail);
        this.parent(PARENT);
    }


    /**
     * {@inheritDoc}
     * Implemented to allow this class to be used in TreeMaps in tests.
     */
    @Override
    public int compareTo(final Resource o) {
        return COMP.compare(this, (StubResource) o);
    }
}

