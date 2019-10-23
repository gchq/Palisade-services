/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.resource.request;

import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request a list of {@link Resource}'s
 * from the resource-service based on a {@link Resource}.
 * For example getting a list of all {@link Resource}'s
 * contained in the given {@link uk.gov.gchq.palisade.resource.impl.DirectoryResource}, the same as an {@code ls} would in linux.
 */
public class GetResourcesByResourceRequest extends Request {
    private Resource resource;

    // no-args constructor required
    public GetResourcesByResourceRequest() {
    }

    /**
     * @param resource the {@link Resource} you want to run an {@code ls} on
     * @return the {@link GetResourcesByResourceRequest}
     */
    public GetResourcesByResourceRequest resource(final Resource resource) {
        requireNonNull(resource, "The resource cannot be set to null.");
        this.resource = resource;
        return this;
    }

    public Resource getResource() {
        requireNonNull(resource, "The resource has not been set.");
        return resource;
    }

    public void setResource(final Resource resource) {
        resource(resource);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetResourcesByResourceRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GetResourcesByResourceRequest that = (GetResourcesByResourceRequest) o;
        return getResource().equals(that.getResource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResource());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetResourcesByResourceRequest.class.getSimpleName() + "[", "]")
                .add("resource=" + resource)
                .toString();
    }
}
