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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.common.User;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * ResourceResponse represents the output for resource-service which will include the Resource identified by the service.
 * This will be forwarded to the policy-service to identify the related Policies associated with the User and Resource.
 * Note that there can any number of ResourceResponse messages generated from a single request.  There will be one message for each
 * of the resources that were found to correspond to this Resource ID.
 * There are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.resource.model.ResourceResponse is the output from the resource-service.
 * uk.gov.gchq.palisade.service.policy.model.PolicyRequest is the input for the policy-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ResourceResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Unique identifier for the user
    private final String userId;
    // Resource ID that that is being asked to access
    private final String resourceId;
    // Json Node representation of the Context
    private final JsonNode context;
    // Json Node representation of the User
    private final JsonNode user;

    /**
     * Resource that has been requested to access
     */
    public final LeafResource resource;

    private ResourceResponse(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") LeafResource resource) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new RuntimeException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new RuntimeException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new RuntimeException("Context cannot be null"));
        this.user = Optional.ofNullable(user).orElseThrow(() -> new RuntimeException("User cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new RuntimeException("Resource cannot be null"));
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

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(user, User.class);
    }

    @Generated
    public LeafResource getResource() {
        return resource;
    }

    /**
     * Builder class for the creation of instances of the ResourceResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * ResourceResponse class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> user -> resource ->
                    new ResourceResponse(userId, resourceId, context, user, resource);
        }

        /**
         * Starter method for the Builder class that uses a ResourceRequest and appends the Rules.
         * This method is called followed by the call to add resource with the IResource interface to create the
         * ResourceResponse class.
         *
         * @param request the request message that was sent to the resource-service
         * @return interface {@link IResource} for the next step in the build.
         */
        public static IResource create(final ResourceRequest request) {
            return create()
                    .withUserId(request.getUserId())
                    .withResourceId(request.getResourceId())
                    .withContextNode(request.getContextNode())
                    .withUserNode(request.getUserNode());
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
             * Adds the user context information.
             *
             * @param context user context for the request.
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
             * @return interface {@link IResource} for the next step in the build.
             */
            default IResource withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            /**
             * Adds the user user information.  Uses a JsonNode string form of the information.
             *
             * @param user for the request.
             * @return class {@link IResource} for the next step in the build.
             */
            IResource withUserNode(JsonNode user);
        }

        /**
         * Adds the resource to this message.
         */
        public interface IResource {
            /**
             * Adds the resource that has been requested to access.
             *
             * @param resource that is requested to access
             * @return class {@link ResourceResponse} for the completed class from the builder.
             */
            ResourceResponse withResource(LeafResource resource);
        }
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
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                user.equals(that.user) &&
                resource.equals(that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context, user, resource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ResourceResponse.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }
}
