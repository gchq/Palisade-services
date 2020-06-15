/*
 * Copyright 2020 Crown Copyright
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.policy.response.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.policy.response.common.domain.User;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the request for the Policy Service
 * The next in the sequence will the response from the Policy Service.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.resource.response.ResourceResponse is the output from the Resource Service
 * uk.gov.gchq.palisade.service.policy.request.PolicyRequest is the input for the Policy Service
 * The key difference in the representation of the attributes.  In this class the all of the objects are needed to
 * process the request.
 */
@JsonDeserialize(builder = PolicyRequest.Builder.class)
public final class PolicyRequest {

    private final String token; // Unique identifier for this specific request end-to-end
    private final User user;  //
    private final Map<String, ResourceMetadata> resources; //map of resources related to this query
    private final Map<String, String> context;  // represents the context information

    private PolicyRequest(String token, User user, Map<String, ResourceMetadata> resources, Map<String, String> context) {
        this.token = token;
        this.user = user;
        this.resources = resources;
        this.context = context;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public Map<String, ResourceMetadata> getResources() {
        return resources;
    }

    @Generated
    public Map<String, String> getContext() {
        return  context;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyRequest)) {
            return false;
        }
        PolicyRequest that = (PolicyRequest) o;
        return token.equals(that.token) &&
                user.equals(that.user) &&
                resources.equals(that.resources) &&
                context.equals(that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, user, resources, context);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PolicyRequest.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("user=" + user)
                .add("resources=" + resources)
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the PolicyRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the PolicyRequest class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private User user;
        private Map<String, ResourceMetadata> resources;
        private Map<String, String> context;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder userJson(User user) {
            this.user = user;
            return this;
        }

        public Builder resource(Map<String, ResourceMetadata> resources) {
            this.resources = resources;
            return this;
        }

        public Builder context(Map<String, String> context) {
            this.context = context;
            return this;
        }

        public PolicyRequest build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(user, "User cannot be null");
            Assert.notNull(resources, "Resource Id cannot be null");
            Assert.notNull(context, "Context  cannot be null");
            return new PolicyRequest(token, user, resources, context);
        }
    }

}


