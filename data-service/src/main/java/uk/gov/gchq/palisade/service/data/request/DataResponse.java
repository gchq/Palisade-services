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
package uk.gov.gchq.palisade.service.data.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This class represents the data that is returned from the authorised data store (key value store/database) when the data service checks if there is an authorised request for the given token and resourceId.
 */

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class DataResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    private final JsonNode resource; // Json Node representation of the Resources
    private final JsonNode rules; // Json Node representation of the Rules

    @JsonCreator
    private DataResponse(
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resource") JsonNode resource,
            final @JsonProperty("rules") JsonNode rules) {

        this.context = Optional.ofNullable(context).orElseThrow(() -> new RuntimeException("Context cannot be null"));
        this.user = Optional.ofNullable(user).orElseThrow(() -> new RuntimeException("User cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new RuntimeException("Resource cannot be null"));
        this.rules = Optional.ofNullable(rules).orElseThrow(() -> new RuntimeException("Rules cannot be null"));
    }

    @Generated
    public String getUserId() throws JsonProcessingException {
        return getUser().getUserId().getId();
    }

    @Generated
    public String getResourceId() throws JsonProcessingException {
        return getResource().getId();
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(this.context, Context.class);
    }

    @Generated
    public JsonNode getContextNode() {
        return context;
    }

    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(this.user, User.class);
    }

    @Generated
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }

    @Generated
    public JsonNode getResourceNode() {
        return resource;
    }

    @Generated
    public Rules getRules() throws JsonProcessingException {
        return MAPPER.treeToValue(this.rules, Rules.class);
    }

    @Generated
    public JsonNode getRulesNode() {
        return rules;
    }

    /**
     * Builder class for the creation of instances of the DataRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * DataRequest class.
         *
         * @return interface {@link IContext} for the next step in the build.
         */
        public static IContext create() {
            return  context -> user -> resource -> rules ->
                    new DataResponse(context, user, resource, rules);
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
             * Adds the user information.
             *
             * @param user for the request.
             * @return class {@link IResource} for the next step in the build.
             */
            default IResource withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            /**
             * Adds the user identified int the system that requested the information.  Uses a JsonNode string form of the information.
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
            default IRules withResource(Resource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            /**
             * Adds the resource that has been requested to access.  Uses a JsonNode string form of the information.
             *
             * @param resource that is requested to access.
             * @return interface {@link IRules} for the next step in the build.
             */
            IRules withResourceNode(JsonNode resource);
        }

        /**
         * Adds the rules associated with this request.
         */
        public interface IRules {
            /**
             * Adds the rules that specify the access.
             *
             * @param rules that apply to this request.
             * @return class {@link DataResponse} for the completed class from the builder.
             */
            default DataResponse withRules(Rules rules) {
                return withRulesNode(MAPPER.valueToTree(rules));
            }

            /**
             * Adds the rules that specify the access.  Uses a JsonNode string form of the information.
             *
             * @param rules that apply to this request.
             * @return class {@link DataResponse} for the completed class from the builder.
             */
            DataResponse withRulesNode(JsonNode rules);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataResponse)) {
            return false;
        }
        DataResponse that = (DataResponse) o;
        return context.equals(that.context) &&
                user.equals(that.user) &&
                resource.equals(that.resource) &&
                rules.equals(that.rules);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(context, user, resource, rules);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", DataResponse.class.getSimpleName() + "[", "]")
                .add("context=" + context)
                .add("user=" + user)
                .add("resource=" + resource)
                .add("rules=" + rules)
                .add(super.toString())
                .toString();
    }
}
