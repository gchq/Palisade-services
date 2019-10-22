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

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request a list of {@link uk.gov.gchq.palisade.resource.Resource}'s
 * from the resource-service based on the type of a {@link uk.gov.gchq.palisade.resource.Resource}.
 * For example getting a list of all {@link uk.gov.gchq.palisade.resource.Resource}'s with the given type.
 */
public class GetResourcesByTypeRequest extends Request {
    private String type;

    // no-args constructor required
    public GetResourcesByTypeRequest() {
    }

    /**
     * @param type the type of the {@link uk.gov.gchq.palisade.resource.Resource}'s that you want to know about
     * @return the {@link GetResourcesByTypeRequest}
     */
    public GetResourcesByTypeRequest type(final String type) {
        requireNonNull(type, "The resource type cannot be set to null.");
        this.type = type;
        return this;
    }

    public String getType() {
        requireNonNull(type, "The resource type has not been set.");
        return type;
    }

    public void setType(final String type) {
        type(type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final GetResourcesByTypeRequest that = (GetResourcesByTypeRequest) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("type", type)
                .toString();
    }
}
