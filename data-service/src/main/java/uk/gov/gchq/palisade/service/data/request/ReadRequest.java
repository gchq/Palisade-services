/*
 * Copyright 2018 Crown Copyright
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

package uk.gov.gchq.palisade.service.data.request;


import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.Generated;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to send a request to the
 * {@link uk.gov.gchq.palisade.service.data.service.DataService} to read a resource.
 */

public class ReadRequest extends Request {
    private String token;
    private LeafResource resource;

    public ReadRequest token(final String token) {
        requireNonNull(token, "The token cannot be set to null.");
        this.token = token;
        return this;
    }

    public ReadRequest resource(final LeafResource resource) {
        requireNonNull(resource, "The resource cannot be set to null.");
        this.resource = resource;
        return this;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public void setToken(final String token) {
        this.token = token;
    }

    @Generated
    public LeafResource getResource() {
        return resource;
    }

    @Generated
    public void setResource(final LeafResource resource) {
        this.resource = resource;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ReadRequest that = (ReadRequest) o;
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
        final StringBuilder sb = new StringBuilder("ReadRequest{");
        sb.append("token='").append(token).append('\'');
        sb.append(", resource=").append(resource);
        sb.append('}');
        return sb.toString();
    }
}
