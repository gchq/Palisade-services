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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request the {@link uk.gov.gchq.palisade.service.request.DataRequestConfig}
 */
public class GetDataRequestConfig extends Request {
    private RequestId token;
    private Resource resource;

    @Generated
    public GetDataRequestConfig token(final RequestId requestId) {
        requireNonNull(requestId, "The request id cannot be set to null.");
        this.setToken(requestId);
        return this;
    }

    @Generated
    public GetDataRequestConfig resource(final Resource resource) {
        requireNonNull(resource, "The resource cannot be set to null.");
        this.setResource(resource);
        return this;
    }

    @Generated
    public RequestId getToken() {
        return token;
    }

    @Generated
    public void setToken(final RequestId token) {
        requireNonNull(token);
        this.token = token;
    }

    @Generated
    public Resource getResource() {
        return resource;
    }

    @Generated
    public void setResource(final Resource resource) {
        requireNonNull(resource);
        this.resource = resource;
    }

    @Override
    @Generated
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
        final GetDataRequestConfig that = (GetDataRequestConfig) o;
        return Objects.equals(token, that.token) &&
                Objects.equals(resource, that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), token, resource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", GetDataRequestConfig.class.getSimpleName() + "[", "]")
                .add("token=" + token)
                .add("resource=" + resource)
                .toString();
    }
}
