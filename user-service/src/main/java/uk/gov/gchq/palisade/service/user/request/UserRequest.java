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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This step in the sequence is the request to the User Service to identify the user associated with
 * the user id given in this original request.
 * The next in the sequence will be the response from the User Service
 * Note there are two class that represent effectively the same data where each has a different purpose.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that came into Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input/request for the User Service
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class UserRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public final String userId;  //Unique identifier for the user
    private final String resourceId;  //Unique identifier for the resource that that is being asked to access
    private final JsonNode context;  // represents the context information

    @JsonCreator
    private UserRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context) {

        Assert.notNull(userId, "User cannot be null");
        Assert.notNull(resourceId, "Resource cannot be null");
        Assert.notNull(context, "Context cannot be null");

        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;
    }


    public String getResourceId() {
        return resourceId;
    }

    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
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
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", UserRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {

        public static IUser create() {
            return user -> resource -> context ->
                    new UserRequest(user, resource, context);
        }

        interface IUser {
            IResource withUser(String userId);

        }

        interface IResource {
            IContext withResource(String resourceId);
        }


        interface IContext {
            default UserRequest withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            UserRequest withContextNode(JsonNode context);
        }
    }

}