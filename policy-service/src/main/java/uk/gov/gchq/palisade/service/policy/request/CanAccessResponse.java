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

package uk.gov.gchq.palisade.service.policy.request;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * The purpose of this class is to wrap the response of the Policy store's can access requests.
 */
public class CanAccessResponse {
    private Collection<LeafResource> canAccessResources;

    public CanAccessResponse() {
        // no-args constructor needed for serialization only
    }

    @Generated
    public CanAccessResponse canAccessResources(final Collection<LeafResource> canAccessResources) {
        requireNonNull(canAccessResources, "The can access resources collection cannot be set to null.");
        this.setCanAccessResources(canAccessResources);
        return this;
    }

    @Generated
    public Collection<LeafResource> getCanAccessResources() {
        return canAccessResources;
    }

    @Generated
    public void setCanAccessResources(final Collection<LeafResource> canAccessResources) {
        requireNonNull(canAccessResources);
        this.canAccessResources = canAccessResources;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CanAccessResponse)) {
            return false;
        }
        final CanAccessResponse that = (CanAccessResponse) o;
        return Objects.equals(canAccessResources, that.canAccessResources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canAccessResources);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", CanAccessResponse.class.getSimpleName() + "[", "]")
                .add("canAccessResources=" + canAccessResources)
                .toString();
    }
}
