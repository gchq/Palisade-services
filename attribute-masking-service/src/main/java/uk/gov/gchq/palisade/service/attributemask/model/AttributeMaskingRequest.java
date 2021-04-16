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
package uk.gov.gchq.palisade.service.attributemask.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Generated;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.user.User;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The AttributeMaskingRequest is the input for attribute-masking-service for preliminary processing and routing of the data.
 * AttributeMaskingResponse is the output for this service which will have the redacted data schema that is to be
 * provided to the client.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * The uk.gov.gchq.palisade.service.policy.model.PolicyResponse is the output from the Policy Service.
 * The AttributeMaskingRequest is the input for the Attribute-Masking Service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class AttributeMaskingRequest {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource ID that that is being asked to access
    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    private final LeafResource resource; // Leaf Resource that will be masked by the service
    private final JsonNode rules; // Json Node representation of the Rules

    @JsonCreator
    private AttributeMaskingRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") LeafResource resource,
            final @JsonProperty("rules") JsonNode rules) {
        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("userId cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("resourceId cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("context cannot be null"));
        this.user = Optional.ofNullable(user).orElseThrow(() -> new IllegalArgumentException("user cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("resource cannot be null"));
        this.rules = Optional.ofNullable(rules).orElseThrow(() -> new IllegalArgumentException("rules cannot be null"));
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
        return MAPPER.treeToValue(this.context, Context.class);
    }

    @Generated
    @JsonIgnore
    public JsonNode getContextNode() {
        return context;
    }

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(this.user, User.class);
    }

    @Generated
    public LeafResource getResource() {
        return resource;
    }

    @Generated
    public Rules getRules() throws JsonProcessingException {
        return MAPPER.treeToValue(this.rules, Rules.class);
    }

    @Generated
    @JsonIgnore
    public JsonNode getRulesNode() {
        return rules;
    }

    /**
     * Builder class for the creation of instances of the AttributeMaskingRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AttributeMaskingRequest class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> user -> resource -> rules ->
                    new AttributeMaskingRequest(userId, resourceId, context, user, resource, rules);
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
             * @param resource that is requested to access.
             * @return interface {@link IRules} for the next step in the build.
             */
            IRules withResource(LeafResource resource);
        }

        /**
         * Adds the rules associated with this request.
         */
        public interface IRules {
            /**
             * Adds the rules that specify the access.
             *
             * @param rules that apply to this request.
             * @return class {@link AttributeMaskingRequest} for the completed class from the builder.
             */
            default AttributeMaskingRequest withRules(Rules rules) {
                return withRulesNode(MAPPER.valueToTree(rules));
            }

            /**
             * Adds the rules that specify the access.  Uses a JsonNode string form of the information.
             *
             * @param rules that apply to this request.
             * @return class {@link AttributeMaskingRequest} for the completed class from the builder.
             */
            AttributeMaskingRequest withRulesNode(JsonNode rules);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttributeMaskingRequest)) {
            return false;
        }
        AttributeMaskingRequest that = (AttributeMaskingRequest) o;
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
        return new StringJoiner(", ", AttributeMaskingRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("user=" + user)
                .add("resources=" + resource)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }
}
