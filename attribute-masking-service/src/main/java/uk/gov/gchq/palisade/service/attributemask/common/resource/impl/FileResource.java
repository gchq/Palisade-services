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

package uk.gov.gchq.palisade.service.attributemask.common.resource.impl;

import uk.gov.gchq.palisade.service.attributemask.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.attributemask.common.resource.AbstractLeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.ConnectionDetail;
import uk.gov.gchq.palisade.service.attributemask.common.resource.ParentResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.Resource;

import java.io.Serializable;
import java.util.Map;

/**
 * A FileResource is the Palisade representation of a file, which extends a LeafResource.
 * {@code eg. "file:/dev/Palisade/pom.xml" = System "/" -> Directory "/dev/" -> Directory "/dev/Palisade/" -> File "/dev/Palisade/pom.xml" }
 */
@RegisterJsonSubType(Resource.class)
public class FileResource extends AbstractLeafResource {
    private static final long serialVersionUID = 1L;

    public FileResource() {
        //no-args constructor needed for serialisation only
    }

    @Override
    public FileResource id(final String id) {
        return (FileResource) super.id(id);
    }

    @Override
    public FileResource type(final String type) {
        return (FileResource) super.type(type);
    }

    @Override
    public FileResource serialisedFormat(final String serialisedFormat) {
        return (FileResource) super.serialisedFormat(serialisedFormat);
    }

    @Override
    public FileResource connectionDetail(final ConnectionDetail connectionDetail) {
        return (FileResource) super.connectionDetail(connectionDetail);
    }

    @Override
    public FileResource attributes(final Map<String, Serializable> attributes) {
        return (FileResource) super.attributes(attributes);
    }

    @Override
    public FileResource attribute(final String attributeKey, final Serializable attributeValue) {
        return (FileResource) super.attribute(attributeKey, attributeValue);
    }

    @Override
    public FileResource parent(final ParentResource parent) {
        return (FileResource) super.parent(parent);
    }
}
