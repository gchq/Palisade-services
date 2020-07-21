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
package uk.gov.gchq.palisade.service.queryscope.request;

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

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the output for query-scope-service with the filtered version of the request.
 * Next in the sequence will be the input for the result-service which will construct a response to the client.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.queryscope.request.QueryScopeResponse is the output from the query-scope-service.
 * uk.gov.gchq.palisade.service.results.request.ResultsRequest is the input for the results-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class QueryScopeResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource ID that that is being asked to access
    private final JsonNode context;  // Json Node representation of the Context

    /**
     * Resource after it has been processed.  This will be information that has been
     * redacted.
     */
    public final JsonNode resource; // Masked resource metadata

    @JsonCreator
    private QueryScopeResponse(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("resource") JsonNode resource) {

        Assert.notNull(userId, "User ID cannot be null");
        Assert.notNull(resourceId, "Resource ID cannot be null");
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(resource, "Resource cannot be null");

        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;
        this.resource = resource;
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
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }

    /**
     * Builder class for the creation of instances of the QueryScopeResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * QueryScopeResponse class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> resource ->
                    new QueryScopeResponse(userId, resourceId, context, resource);
        }

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * QueryScopeResponse class.
         * Starter method for the Builder class that uses a QueryScopeRequests and appends the redacted version of the resource.
         * This method is called followed by the call to add user with the IUserId interface to create the
         * QueryScopeResponse class.
         *
         * @param request message that was sent to the query-scope-service
         * @return interface  {@link IResourceId} for the next step in the build.
         */
        public static QueryScopeResponse create(final QueryScopeRequest request) {
            return create()
                    .withUserId(request.getUserId())
                    .withResourceId(request.getResourceId())
                    .withContextNode(request.getContextNode())
                    .withResourceNode(request.getResourceNode());
        }

        /**
         * Adds the user ID information to the message.
         */
        interface IUserId {
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
        interface IResourceId {
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
        interface IContext {
            /**
             * Adds the user context information.
             *
             * @param context user context for the request.
             * @return interface {@link IResource} for the next step in the build.
             */
            default IResource withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             * @return interface {@link IResource} for the next step in the build.
             */
            IResource withContextNode(JsonNode context);
        }

        /**
         * Adds the resource to this message.
         */
        interface IResource {
            /**
             * Adds the resource that has been requested to access.
             *
             * @param resource that is requested to access.
             * @return class {@link QueryScopeResponse} for the completed class from the builder.
             */
            default QueryScopeResponse withResource(LeafResource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            /**
             * Adds the Resource information. Uses a JsonNode String form of the information
             *
             * @param resource resource that has been requested to access
             * @return class {@link QueryScopeResponse} for the completed class from the builder
             */
            QueryScopeResponse withResourceNode(JsonNode resource);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryScopeResponse)) {
            return false;
        }
        QueryScopeResponse that = (QueryScopeResponse) o;
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context) &&
                resource.equals(that.resource);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context, resource);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", QueryScopeResponse.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }
}
