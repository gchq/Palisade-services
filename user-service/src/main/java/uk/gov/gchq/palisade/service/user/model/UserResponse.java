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
package uk.gov.gchq.palisade.service.user.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.Generated;
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This data represents the output for User Service which will include the User identified by the service.
 * This will be forwarded to the Resource Service to identify the Resources associated with this query.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.user.model.UserResponse is the response from the User Service, this contains the returned user.
 * uk.gov.gchq.palisade.service.resource.model.ResourceRequest contains the information from the User Service which is used to find the Resource(s).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class UserResponse {

    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();
    /**
     * Represents the User in the system corresponding to the given useId.
     */
    public final User user;
    private final String userId;  //Unique identifier for the user
    private final String resourceId; //Resource that that is being asked to access
    private final JsonNode context; //Represents the context information as a Json string.

    @JsonCreator
    private UserResponse(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") User user) {

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
        return new StringJoiner(", ", UserResponse.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of instances of the UserResponse. This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * UserResponse class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> user ->
                    new UserResponse(userId, resourceId, context, user);
        }

        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * UserResponse class. This method uses a UserRequest to create the UserResponse and then appends the User.
         * This method is called followed by the call to add user with the IUserId interface to create the
         * UserResponse class.
         *
         * @param request is the request message that has been supplied to the User Service
         * @return interface {@link IUser} for the next step in the build.
         */

        public static IUser create(final UserRequest request) {
            return create()
                    .withUserId(request.getUserId())
                    .withResourceId(request.getResourceId())
                    .withContextNode(request.getContextNode());
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
         * Adds the context information to the message.
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
             * @return a {@link UserResponse} for the completed class from the builder.
             */
            UserResponse withUser(User user);
        }
    }
}
