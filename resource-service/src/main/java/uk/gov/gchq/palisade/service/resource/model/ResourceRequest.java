/*
 * Copyright 2018-2021 Crown Copyright
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
package uk.gov.gchq.palisade.service.resource.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.resource.common.Context;
import uk.gov.gchq.palisade.service.resource.common.Generated;
import uk.gov.gchq.palisade.service.resource.common.user.User;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The ResourceRequest is the input for resource-service to identify the Resource associated with the given Resource ID.
 * ResourceResponse is the output for this service which will include the Resource identified by the service.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.response.UserResponse is the response with the data from user-service included.
 * uk.gov.gchq.palisade.service.resource.model.ResourceRequest is the input for the resource-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class ResourceRequest {

    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    // Unique identifier for the user
    private final String userId;
    // Json Node representation of Context
    private final JsonNode context;
    // Json Node representation of the User
    private final JsonNode user;

    /**
     * Resource Id of the resource requested
     */
    public final String resourceId;

    @JsonCreator
    private ResourceRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new RuntimeException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new RuntimeException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new RuntimeException("Context cannot be null"));
        this.user = Optional.ofNullable(user).orElseThrow(() -> new RuntimeException("User cannot be null"));
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
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(context, Context.class);
    }

    @JsonIgnore
    @Generated
    public JsonNode getContextNode() {
        return context;
    }

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(user, User.class);
    }

    @JsonIgnore
    @Generated
    public JsonNode getUserNode() {
        return user;
    }

    /**
     * Builder class for the creation of instances of the ResourceRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * ResourceRequest class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> user ->
                    new ResourceRequest(userId, resourceId, context, user);
        }

        /**
         * Adds the user ID information to the message.
         */
        public interface IUserId {
            /**
             * Adds the user ID.
             *
             * @param userId user ID for the request.
             * @return interface {@link IResourceId} for the next step in the build.
             */
            IResourceId withUserId(String userId);
        }

        /**
         * Adds the resource ID information to the message.
         */
        public interface IResourceId {
            /**
             * Adds the resource ID.
             *
             * @param resourceId resource ID for the request.
             * @return interface {@link IContext} for the next step in the build.
             */
            IContext withResourceId(String resourceId);
        }

        /**
         * Adds the user context information to the message.
         */
        public interface IContext {
            /**
             * Adds the user context information.
             *
             * @param context user context for the request.
             * @return interface {@link IUser} for the next step in the build.
             */
            default IUser withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information. Uses a JsonNode string form of the information.
             *
             * @param context user context information for the request.
             * @return interface {@link IUser} for the next step in the build.
             */
            IUser withContextNode(JsonNode context);
        }

        /**
         * Adds the user information to the message.
         */
        public interface IUser {
            /**
             * Adds the user to this message.
             *
             * @param user for the request.
             * @return interface {@link ResourceRequest} for the completed class from the builder.
             */
            default ResourceRequest withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            /**
             * Adds the user to this message.  Uses a JsonNode string form of the information.
             *
             * @param user for the request.
             * @return interface {@link ResourceRequest} for the completed class from the builder.
             */
            ResourceRequest withUserNode(JsonNode user);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceRequest)) {
            return false;
        }
        ResourceRequest that = (ResourceRequest) o;
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                user.equals(that.user);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context, user);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add(super.toString())
                .toString();
    }
}
