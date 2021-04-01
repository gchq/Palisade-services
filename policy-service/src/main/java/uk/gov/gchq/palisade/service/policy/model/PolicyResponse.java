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
package uk.gov.gchq.palisade.service.policy.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.policy.common.Context;
import uk.gov.gchq.palisade.service.policy.common.Generated;
import uk.gov.gchq.palisade.service.policy.common.User;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * PolicyResponse represents the output for policy-service which will include the Rules to implement with this Resource.
 * This will be forwarded to the attribute-masking-service for preliminary processing and routing of the data.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.policy.response.PolicyResponse is the output from the policy-service.
 * uk.gov.gchq.palisade.service.attributemasking.message.AttributeMaskingRequest is the input for the attribute-masking-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class PolicyResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource ID that that is being asked to access
    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    private final JsonNode resource; // Json Node representation of the Resources
    private final Rules rules;  //Holds all of the Rules applicable to this request

    @JsonCreator
    private PolicyResponse(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") JsonNode resource,
            final @JsonProperty("rules") Rules rules) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("Context cannot be null"));
        this.user = Optional.ofNullable(user).orElseThrow(() -> new IllegalArgumentException("User cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("Resource cannot be null"));
        this.rules = Optional.ofNullable(rules).orElseThrow(() -> new IllegalArgumentException("Rules cannot be null"));
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
    public Context getContext() {
        try {
            return MAPPER.treeToValue(this.context, Context.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to get Context", e);
        }
    }

    @Generated
    @JsonIgnore
    JsonNode getContextNode() {
        return this.context;
    }

    @Generated
    public User getUser() {
        try {
            return MAPPER.treeToValue(this.user, User.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to get User", e);
        }
    }

    @Generated
    @JsonIgnore
    JsonNode getUserNode() {
        return this.user;
    }


    public LeafResource getResource() {
        try {
            return MAPPER.treeToValue(this.resource, LeafResource.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to get Resource", e);
        }
    }

    @Generated
    @JsonIgnore
    JsonNode getResourceNode() {
        return this.resource;
    }

    @Generated
    public Rules getRules() {
        return rules;
    }


    /**
     * Builder class for the creation of instances of the PolicyResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * PolicyResponse class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> user -> resource -> rules ->
                    new PolicyResponse(userId, resourceId, context, user, resource, rules);
        }

        /**
         * Starter method for the Builder class that uses a PolicyRequest and appends the Rules.
         * This method is called followed by the call to add rules with the IRules interface to create the
         * PolicyResponse class.
         *
         * @param request message that has been sent to the policy-service
         * @return interface {@link IRules} for the next step in the build.
         */
        public static IRules create(final PolicyRequest request) {
            return create()
                    .withUserId(request.getUserId())
                    .withResourceId(request.getResourceId())
                    .withContextNode(request.getContextNode())
                    .withUserNode(request.getUserNode())
                    .withResourceNode(request.getResourceNode());
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
             * Adds the user context information.  Uses a JsonNode string form of the information.
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
             * Adds the user user information.
             *
             * @param user for the request.
             * @return class {@link IResource} for the next step in the build.
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
             * @return class {@link IRules} for the next step in the build.
             */
            default IRules withResource(Resource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            /**
             * Adds the resource that has been requested to access.  Uses a JsonNode string form of the information.
             *
             * @param resource that is requested to access
             * @return class {@link IRules} for the next step in the build.
             */
            IRules withResourceNode(JsonNode resource);
        }

        /**
         * Adds the rules to this message.
         */
        public interface IRules {
            /**
             * Adds the rules that has apply to this request.
             *
             * @param rules that apply to this request.
             * @return class {@link PolicyResponse} for the completed class from the builder.
             */
            PolicyResponse withRules(Rules rules);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyResponse)) {
            return false;
        }
        PolicyResponse that = (PolicyResponse) o;
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                user.equals(that.user) &&
                resource.equals(that.resource) &&
                rules.equals(that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context, user, resource, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PolicyResponse.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add("resource=" + resource)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }
}
