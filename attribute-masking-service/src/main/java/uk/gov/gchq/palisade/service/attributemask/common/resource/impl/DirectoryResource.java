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

import uk.gov.gchq.palisade.service.attributemask.common.Generated;
import uk.gov.gchq.palisade.service.attributemask.common.RegisterJsonSubType;
import uk.gov.gchq.palisade.service.attributemask.common.resource.AbstractResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.ChildResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.ParentResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.Resource;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A DirectoryResource is a Palisade representation of a directory and can have Children and Parent resources
 * {@code eg. "file:/dev/Palisade/pom.xml" = System "/" -> Directory "/dev/" -> Directory "/dev/Palisade/" -> File "/dev/Palisade/pom.xml" }
 */
@RegisterJsonSubType(Resource.class)
public class DirectoryResource extends AbstractResource implements ChildResource, ParentResource {
    private static final long serialVersionUID = 1L;

    private ParentResource parent;

    public DirectoryResource() {
        //no-args constructor needed for serialization only
    }

    @Override
    public DirectoryResource id(final String id) {
        if (id.endsWith("/")) {
            return (DirectoryResource) super.id(id);
        } else {
            return (DirectoryResource) super.id(id + "/");
        }
    }

    public DirectoryResource parent(final ParentResource parent) {
        this.setParent(parent);
        return this;
    }

    @Override
    @Generated
    public ParentResource getParent() {
        return parent;
    }

    @Override
    @Generated
    public void setParent(final ParentResource parent) {
        requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DirectoryResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final DirectoryResource that = (DirectoryResource) o;
        return Objects.equals(parent, that.parent);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), parent);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", DirectoryResource.class.getSimpleName() + "[", "]")
                .add("parent=" + parent)
                .add(super.toString())
                .toString();
    }
}
