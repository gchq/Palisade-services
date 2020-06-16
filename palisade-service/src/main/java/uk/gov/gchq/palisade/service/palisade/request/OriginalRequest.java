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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the original request.  Next in the sequence is User Service where this data will be the
 * input (or in other words a request) for a User.
 * Note there are two class that represent effectively the same data where each represents a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input for the User Service
 */

@JsonDeserialize(builder = OriginalRequest.Builder.class)
public final class OriginalRequest {

    private final String token; // Unique identifier for this specific request end-to-end
    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource that that is being asked to access
    private final Map<String, String> context; //Relevant information about the request.

    private OriginalRequest(final String token, final String userId, final String resourceId, final Map<String, String> context) {
        this.token = token;
        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public String getUserId() {
        return userId;
    }

    @Generated
    public String getResourceID() {
        return resourceId;
    }

    @Generated
    public Map<String, String> getContext() {
        return context;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OriginalRequest)) {
            return false;
        }
        OriginalRequest that = (OriginalRequest) o;
        return token.equals(that.token) &&
                userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, userId, resourceId, context);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", OriginalRequest.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }



    /**
     * Builder class for the creation of instances of the OriginalRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the UserRequest class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private String userId;
        private String resourceId;
        private Map<String, String> context;



        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        public Builder resourceId(final String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder context(final Map<String, String> context) {
            this.context = context;
            return this;
        }

        public OriginalRequest build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(userId, "User Id cannot be null");
            Assert.notNull(resourceId, "Resource Id cannot be null");
            Assert.notNull(context, "Context  cannot be null");
            Assert.notEmpty(context, "Context  cannot be empty");
            return new OriginalRequest(token, userId, resourceId, context);
        }
    }

}
