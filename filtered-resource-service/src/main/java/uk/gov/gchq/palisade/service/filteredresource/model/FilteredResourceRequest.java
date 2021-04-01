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
package uk.gov.gchq.palisade.service.filteredresource.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.Generated;
import uk.gov.gchq.palisade.service.filteredresource.common.resource.LeafResource;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The FilteredResourceRequest is the input for results-service where the resource is queued-up ready for the client's request
 * to retrieve the Resource.
 * TopicOffsetMessage is the output for this service which will be send the client the information needed to
 * retrieve this data.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse is the output from the attribute-masking-service.
 * uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest is the input for the filtered-resource-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class FilteredResourceRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId;  // Unique identifier for the user
    private final String resourceId;  // Resource ID that that is being asked to access
    private final JsonNode context;  // Json Node representation of the Context
    private final JsonNode resource; // Json Node representation of the Resource

    @JsonCreator
    private FilteredResourceRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("resource") JsonNode resource) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("Context cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("Resource cannot be null"));
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
    public LeafResource getResource() throws JsonProcessingException {
        return MAPPER.treeToValue(this.resource, LeafResource.class);
    }

    @Generated
    @JsonIgnore
    public JsonNode getResourceNode() {
        return resource;
    }

    /**
     * Builder class for the creation of instances of the FilteredResourceRequest.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * FilteredResourceRequest class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> resource ->
                    new FilteredResourceRequest(userId, resourceId, context, resource);
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
        public interface IResource {

            /**
             * Adds the user context information.
             *
             * @param resource for the request.
             * @return interface {@link IResource} for the next step in the build.
             */
            default FilteredResourceRequest withResource(LeafResource resource) {
                return withResourceNode(MAPPER.valueToTree(resource));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             * @return interface {@link IResource} for the next step in the build.
             */
            FilteredResourceRequest withResourceNode(JsonNode context);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FilteredResourceRequest)) {
            return false;
        }
        FilteredResourceRequest that = (FilteredResourceRequest) o;
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
        return new StringJoiner(", ", FilteredResourceRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }
}
