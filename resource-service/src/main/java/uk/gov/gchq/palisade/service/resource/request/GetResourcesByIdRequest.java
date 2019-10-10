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

import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class    GetResourcesByIdRequest extends Request {

    private String resourceId;

    // no-args constructor required
    public GetResourcesByIdRequest() {
    }

    /**
     * @param resourceId the unique identifier of the resource that you want to {@code ls}
     * @return the {@link GetResourcesByIdRequest}
     */
    public GetResourcesByIdRequest resourceId(final String resourceId) {
        requireNonNull(resourceId, "The resource id cannot be set to null.");
        this.resourceId = resourceId;
        return this;
    }

    public String getResourceId() {
        requireNonNull(resourceId, "The resource id has not been set.");
        return resourceId;
    }

    public void setResourceId(final String resourceId) {
        resourceId(resourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetResourcesByIdRequest)) return false;
        GetResourcesByIdRequest that = (GetResourcesByIdRequest) o;
        return getResourceId().equals(that.getResourceId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResourceId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetResourcesByIdRequest.class.getSimpleName() + "[", "]")
                .add("resourceId='" + resourceId + "'")
                .toString();
    }
}
