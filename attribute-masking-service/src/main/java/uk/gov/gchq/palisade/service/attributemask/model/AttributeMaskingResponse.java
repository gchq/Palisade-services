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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Generated;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.config.ApplicationConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * AttributeMaskingResponse represents the output for attribute-masking-service which will include redacted data schema that is to be
 * provided to the client.
 * This will be forwarded to the filtered-resource-service in preparation for the client's request for the related Resource.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * The AttributeMaskingResponse is the output from the Attribute-Masking Service.
 * The uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest is the input for the Filtered-Resource Service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NAME)
public final class AttributeMaskingResponse {

    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource ID that that is being asked to access
    private final JsonNode context;  // Json Node representation of the Context

    /**
     * Resource after it has been processed.  This will be information that has been
     * redacted.
     */
    private final LeafResource resource; // Masked resource metadata

    @JsonCreator
    private AttributeMaskingResponse(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("resource") LeafResource resource) {
        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("userId cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("resourceId cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("context cannot be null"));
        this.resource = Optional.ofNullable(resource).orElseThrow(() -> new IllegalArgumentException("resource cannot be null"));
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
    public LeafResource getResource() {
        return resource;
    }

    /**
     * Builder class for the creation of instances of the AttributeMaskingResponse.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AttributeMaskingResponse class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> resource ->
                    new AttributeMaskingResponse(userId, resourceId, context, resource);
        }

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AttributeMaskingResponse class.
         * Starter method for the Builder class that uses a AttributeMaskingRequest and appends the redacted version of the resource.
         * This method is called followed by the call to add user with the IUserId interface to create the
         * AttributeMaskingResponse class.
         *
         * @param request message that was sent to the attribute-masking-service
         * @return interface  {@link IResourceId} for the next step in the build.
         */
        public static IResource create(final AttributeMaskingRequest request) {
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
             * Adds the resource that has been requested to access.
             *
             * @param resource that is requested to access.
             * @return class {@link AttributeMaskingResponse} for the completed class from the builder.
             */
            AttributeMaskingResponse withResource(LeafResource resource);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttributeMaskingResponse)) {
            return false;
        }
        AttributeMaskingResponse that = (AttributeMaskingResponse) o;
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
        return new StringJoiner(", ", AttributeMaskingResponse.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add("resource=" + resource)
                .add(super.toString())
                .toString();
    }
}
