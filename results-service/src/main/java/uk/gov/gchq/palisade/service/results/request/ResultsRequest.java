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
package uk.gov.gchq.palisade.service.results.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.results.request.common.domain.User;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the input for results-service to construct a response to the original request from the client.
 * Next in the sequence will be the output for the result-service which will be a response to the client.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.queryscope.response.QueryScopeResponse is the output from the query-scope-service.
 * uk.gov.gchq.palisade.service.results.request.ResultsRequest is the input for the results-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ResultsRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode user;  //Json Node representation of the User
    private final JsonNode resources; // Json Node representation of the Resources
    private final JsonNode rules; // Json Node representation of the Rules

    @JsonCreator
    private ResultsRequest(
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("user") JsonNode user,
            final @JsonProperty("resources") JsonNode resources,
            final @JsonProperty("rules") JsonNode rules) {


        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(resources, "Resources cannot be null");
        Assert.notNull(rules, "Rules cannot be null");

        this.context = context;
        this.user = user;
        this.resources = resources;
        this.rules = rules;
    }

    @Generated
    public Context getContext() throws JsonProcessingException {
        return MAPPER.treeToValue(this.context, Context.class);
    }


    @Generated
    public User getUser() throws JsonProcessingException {
        return MAPPER.treeToValue(this.user, User.class);
    }

    @Generated
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resources, LeafResource.class);
    }

    @Generated
    public Rules getRules() throws JsonProcessingException {
        return MAPPER.treeToValue(this.rules, Rules.class);
    }


    /**
     * Builder class for the creation of instances of the ResultsRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        private JsonNode context;
        private JsonNode user;
        private JsonNode resources;
        private JsonNode rules;

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * PolicyRequest class.
         *
         * @return fully constructed  {@link ResultsRequest} instance.
         */
        public static IContext create() {
            return context -> user -> resources -> rules ->
                    new ResultsRequest(context, user, resources, rules);
        }

        /**
         * Adds the user context information to the response message.
         */
        interface IContext {
            /**
             * Adds the user context information.
             *
             * @param context information about the user in context to this message.
             * @return class {@link IUser} for the next step in the build.
             */
            default IUser withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.
             *
             * @param context information about the user in context to this message.
             * @return class {@link IUser} for the next step in the build.
             */
            IUser withContextNode(JsonNode context);

        }

        /**
         * Adds the user associated with this request to the message.
         */
        interface IUser {
            /**
             * Adds the user user information.
             *
             * @param user information about the user in context to this message.
             * @return interface {@link IResource} for the completed class from the builder.
             */
            default IResource withUser(User user) {
                return withUserNode(MAPPER.valueToTree(user));
            }

            /**
             * Adds the user user information.
             *
             * @param user information about the user in context to this message.
             * @return interface {@link IResource} for the completed class from the builder.
             */
            IResource withUserNode(JsonNode user);
        }

        /**
         * Adds the resource associated with this message.
         */
        interface IResource {
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
             * Adds the resource that has been requested to access.
             *
             * @param resource that is requested to access.
             * @return interface {@link IRules} for the next step in the build.
             */
            IRules withResourceNode(JsonNode resource);
        }

        /**
         * Adds the rules associated with this response.
         */
        interface IRules {
            /**
             * Adds the rules that has been requested to access.
             *
             * @param rules that apply to this request.
             * @return class {@link ResultsRequest} for the completed class from the builder.
             */
            default ResultsRequest withRules(Rules rules) {
                return withResourceNode(MAPPER.valueToTree(rules));
            }

            /**
             * Adds the rules that has been requested to access.
             *
             * @param rules that apply to this request.
             * @return class {@link ResultsRequest} for the completed class from the builder.
             */
            ResultsRequest withResourceNode(JsonNode rules);
        }

    }

}
