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
import uk.gov.gchq.palisade.service.request.Policy;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This class is used in the request to set a {@link Policy} for a resource type.
 */
@JsonIgnoreProperties(value = {"originalRequestId"})
public class SetTypePolicyRequest extends Request {
    private String type;
    private Policy policy;

    public SetTypePolicyRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param type the {@link String} to set the {@link Policy} for
     * @return the {@link SetTypePolicyRequest}
     */
    @Generated
    public SetTypePolicyRequest type(final String type) {
        this.type = Optional.ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Type cannot be null"));
        return this;
    }

    /**
     * @param policy the {@link Policy} to set for the resource type
     * @return the {@link SetTypePolicyRequest}
     */
    @Generated
    public SetTypePolicyRequest policy(final Policy policy) {
        this.policy = Optional.ofNullable(policy).orElseThrow(() -> new IllegalArgumentException("Policy cannot be null"));
        return this;
    }

    @Generated
    public String getType() {
        return type;
    }

    @Generated
    public void setType(final String type) {
        this.type = Optional.ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Type cannot be null"));
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
        throw new ForbiddenException("Should not call SetTypePolicyRequest.getOriginalRequestId()");
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetTypePolicyRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SetTypePolicyRequest that = (SetTypePolicyRequest) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(policy, that.policy);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, policy);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", SetTypePolicyRequest.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("policy=" + policy)
                .toString();
    }
}
