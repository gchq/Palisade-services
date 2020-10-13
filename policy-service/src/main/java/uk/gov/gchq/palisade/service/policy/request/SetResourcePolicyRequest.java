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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.exception.ForbiddenException;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used in the request to set a {@link Policy} for a {@link Resource}.
 * That resource may be signifying a file, stream, directory or the system
 * (policy is applied to all requests to the Palisade system).
 */
@JsonIgnoreProperties(value = {"originalRequestId"})
public class SetResourcePolicyRequest extends Request {
    private Resource resource;
    private Policy policy;

    public SetResourcePolicyRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param resource the {@link Resource} to set the {@link Policy} for
     * @return the {@link SetResourcePolicyRequest}
     */
    @Generated
    public SetResourcePolicyRequest resource(final Resource resource) {
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("Resource cannot be null"));
        return this;
    }

    /**
     * @param policy the {@link Policy} to set for the {@link Resource}
     * @return the {@link SetResourcePolicyRequest}
     */
    @Generated
    public SetResourcePolicyRequest policy(final Policy policy) {
        this.policy = Optional.ofNullable(policy).orElseThrow(() -> new IllegalArgumentException("Policy cannot be null"));
        return this;
    }

    @Generated
    public Resource getResource() {
        return resource;
    }

    @Generated
    public void setResource(final Resource resource) {
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("Resource cannot be null"));
    }

    @Generated
    public Policy getPolicy() {
        return policy;
    }

    @Generated
    public void setPolicy(final Policy policy) {
        this.policy = Optional.ofNullable(policy).orElseThrow(() -> new IllegalArgumentException("Policy cannot be null"));
    }

    @Override
    public RequestId getOriginalRequestId() {
        throw new ForbiddenException("Should not call SetResourcePolicyRequest.getOriginalRequestId()");
    }

    @Override
    public void setOriginalRequestId(final RequestId originalRequestId) {
        throw new ForbiddenException("Should not call SetResourcePolicyRequest.setOriginalRequestId()");
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetResourcePolicyRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SetResourcePolicyRequest request = (SetResourcePolicyRequest) o;
        return Objects.equals(resource, request.resource) &&
                Objects.equals(policy, request.policy);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, policy);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", SetResourcePolicyRequest.class.getSimpleName() + "[", "]")
                .add("resource=" + resource)
                .add("policy=" + policy)
                .toString();
    }
}
