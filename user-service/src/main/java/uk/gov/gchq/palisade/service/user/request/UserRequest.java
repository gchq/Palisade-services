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
package uk.gov.gchq.palisade.service.user.request;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Generated;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the original request, but is to be used as the request for finding the User associated with
 * this request.
 * The next in the sequence will the response from the User Service which will include the information about the user.
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input for the User Service
 * Note the context was converted to a String for sending from Palisade Service to User Service, but has not been
 * converted back as it is not being used in User Service.
 */
@JsonDeserialize(builder = UserRequest.Builder.class)
public final class UserRequest {

    private final String token; // Unique identifier for this specific request end-to-end
    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource that that is being asked to access
    private final String contextJson;  // represents the context information as a Json string of a Map<String, String>

   //?? should we have this
    //My take on this is no.  If we need it make it part of the constructor.
    @JsonIgnore
    private Map<String, String> context = null;


    private UserRequest(final String token, final String userId, final String resourceId, final String contextJson) {
        this.token = token;
        this.userId = userId;
        this.resourceId = resourceId;
        this.contextJson = contextJson;
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
    public String getResourceId() {
        return resourceId;
    }

    @Generated
    public String getContextJson() {
        return contextJson;
    }

    public Map<String, String> getContext() throws JsonProcessingException {
        if (context == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            //???assuming it is a HashMap but treating it as a Map
            context = objectMapper.readValue(contextJson, HashMap.class);
        }
        return context;
    }


    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRequest)) {
            return false;
        }
        UserRequest that = (UserRequest) o;
        return token.equals(that.token) &&
                userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                contextJson.equals(that.contextJson) &&
                context.equals(that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, userId, resourceId, contextJson, context);
    }


    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", UserRequest.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("contextJson='" + contextJson + "'")
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserRequest.  The variant of the Builder Pattern is
     * meant to be used by first populating the Builder class and then us this to create the UserRequest class.
     */
    @JsonPOJOBuilder
    public static class Builder {
        private String token;
        private String userId;
        private String resourceId;
        private String contextJson;


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

        public Builder context(final String contextJson) {
            this.contextJson = contextJson;
            return this;
        }

        public UserRequest build() {
            Assert.notNull(token, "Token Id cannot be null");
            Assert.notNull(userId, "User cannot be null");
            Assert.notNull(resourceId, "Resource Id cannot be null");
            Assert.notNull(contextJson, "Context  cannot be null");
            return new UserRequest(token, userId, resourceId, contextJson);
        }
    }
}