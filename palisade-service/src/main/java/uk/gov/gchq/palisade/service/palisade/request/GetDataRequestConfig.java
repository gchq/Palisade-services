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
package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class GetDataRequestConfig extends Request {
    private RequestId requestId;
    private Resource resource;

    public RequestId getRequestId() {
        requireNonNull(requestId, "The request id has not been set.");
        return requestId;
    }

    public GetDataRequestConfig requestId(final RequestId requestId) {
        requireNonNull(requestId, "The request id cannot be set to null.");
        this.requestId = requestId;
        return this;
    }

    public void setRequestId(final RequestId requestId) {
        requestId(requestId);
    }

    public Resource getResource() {
        requireNonNull(resource, "The resource has not been set.");
        return resource;
    }

    public GetDataRequestConfig resource(final Resource resource) {
        requireNonNull(resource, "The resource cannot be set to null.");
        this.resource = resource;
        return this;
    }

    public void setResource(final Resource resource) {
        resource(resource);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetDataRequestConfig)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GetDataRequestConfig that = (GetDataRequestConfig) o;
        return getRequestId().equals(that.getRequestId()) &&
                getResource().equals(that.getResource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getRequestId(), getResource());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetDataRequestConfig.class.getSimpleName() + "[", "]")
                .add("requestId=" + requestId)
                .add("resource=" + resource)
                .toString();
    }
}
