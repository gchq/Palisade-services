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
package uk.gov.gchq.palisade.service.user.response;

import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the response from the Use Service.
 * The next in the sequence will the request for Resource Service.
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.resource.request.ResourceRequest is the input for the User Service
 */
public final class  UserResponse {

    private final String token; // Unique identifier for this specific request end-to-end
    private final User user;  //Representation of the User
    private final String resourceId;  //Resource that that is being asked to access
    private final String contextJson;  // represents the context information as a Json string of a Map<String, String>


    private UserResponse(final String token, final User user, final String resourceId, final String contextJson) {
        this.token = token;
        this.user = user;
        this.resourceId = resourceId;
        this.contextJson = contextJson;
    }

    @Generated
    public String getToken() {
        return token;
    }


    @Generated
    public User user() {
        return user;
    }

    @Generated
    public String getResourceId() {
        return resourceId;
    }


    @Generated
    public String getContextJson() {
        return contextJson;
    }
    //Should we have a getter method for User and context?
    //@JSonIgnore
    // User user;
    // @JsonIgnore
    // private Map<String, String> context = null;

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserResponse)) {
            return false;
        }
        UserResponse that = (UserResponse) o;
        return token.equals(that.token) &&
                user.equals(that.user) &&
                resourceId.equals(that.resourceId) &&
                contextJson.equals(that.contextJson);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, user, resourceId, contextJson);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", UserResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("user=" + user)
                .add("resourceId='" + resourceId + "'")
                .add("contextJson='" + contextJson + "'")
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserResponse.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the UserRequest class.
     */
    public static class Builder {
        private String token;
        private User user;
        private String resourceId;
        private String contextJson;



        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder user(final User user) {
            this.user = user;
            return this;
        }

        public Builder resourceId(final String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder context(final String contextJson) {
            this.contextJson = contextJson;
            return this;
        }

        public UserResponse build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(user, "User cannot be null");
            Assert.notNull(resourceId, "Resource Id cannot be null");
            Assert.notNull(contextJson, "Context  cannot be null");
            return new UserResponse(token, user, resourceId, contextJson);
        }
    }


}