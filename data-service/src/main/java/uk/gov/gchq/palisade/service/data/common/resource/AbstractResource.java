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

package uk.gov.gchq.palisade.service.data.common.resource;

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Comparator;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This is a partial implementation of a Resource which provides basic member-variable storage to implement
 * methods required of a Resource.
 * This class is mostly used when deserialisation to a Resource is required, but the interface can't be used.
 */
public abstract class AbstractResource implements Resource {
    private static final Comparator<Resource> COMP = Comparator.comparing(Resource::getId);

    protected String id;

    public AbstractResource() {
    }

    @Generated
    public AbstractResource id(final String id) {
        this.setId(id);
        return this;
    }

    @Override
    @Generated
    public String getId() {
        return id;
    }

    @Override
    @Generated
    public void setId(final String id) {
        requireNonNull(id);
        this.id = id;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractResource)) {
            return false;
        }
        AbstractResource that = (AbstractResource) o;
        return id.equals(that.getId());
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AbstractResource.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add(super.toString())
                .toString();
    }

    @Override
    public int compareTo(final Resource o) {
        return COMP.compare(this, o);
    }

}
