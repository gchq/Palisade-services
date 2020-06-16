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
package uk.gov.gchq.palisade.service.resource.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.resource.response.common.ResourceMetadata;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the  data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This class represents the response from the Resource Service which will add the resources to data set
 * The next in the sequence will the request for Policy Service.
 * Note there are two class that represents the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.resource.response.ResourceResponse is the output from the Resource Service
 * uk.gov.gchq.palisade.service.policy.request.PolicyRequest is the input for the Policy Service
 */
@JsonDeserialize(builder = ResourceResponse.Builder.class)
public final class ResourceResponse {
    private final String token; // Unique identifier for this specific request end-to-end
    private final String userJson;  //JSon string for the User object
    private final Map<String, ResourceMetadata> resources; //map of resources related to this query
    private final String contextJson;  // represents the context information as a Json string of a Map<String, String>

    private ResourceResponse(final String token, final String userJson, final Map<String, ResourceMetadata> resources, final String contextJson) {
        this.token = token;
        this.userJson = userJson;
        this.resources = resources;
        this.contextJson = contextJson;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getUserJson() {
        return userJson;
    }

    @Generated
    public Map<String, ResourceMetadata> getResources() {
        return resources;
    }

    @Generated
    public String getContextJson() {
        return contextJson;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceResponse)) {
            return false;
        }
        ResourceResponse that = (ResourceResponse) o;
        return token.equals(that.token) &&
                userJson.equals(that.userJson) &&
                resources.equals(that.resources) &&
                contextJson.equals(that.contextJson);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, userJson, resources, contextJson);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("userJson='" + userJson + "'")
                .add("resources=" + resources)
                .add("contextJson='" + contextJson + "'")
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the ResourceResponse.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the ResourceResponse class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private String userJson;
        private Map<String, ResourceMetadata> resources;
        private String contextJson;

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder userJson(final String userJson) {
            this.userJson = userJson;
            return this;
        }

        public Builder resource(final Map<String, ResourceMetadata> resources) {
            this.resources = resources;
            return this;
        }

        public Builder context(final String contextJson) {
            this.contextJson = contextJson;
            return this;
        }

        public ResourceResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(userJson, "User cannot be null");
            Assert.notNull(resources, "Resource Id cannot be null");
            Assert.notNull(contextJson, "Context  cannot be null");
            return new ResourceResponse(token, userJson, resources, contextJson);
        }
    }
}
