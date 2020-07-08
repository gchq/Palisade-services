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
package uk.gov.gchq.palisade.service.palisade.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the original request.
 * Next in the sequence is the input for user-service where this data will be used as a request for a User.
 * Note there are two classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.request.OriginalRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the input for the User Service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class OriginalRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId;  //Unique identifier for the user
    private final String resourceId;  //Resource that that is being asked to access
    private final JsonNode context; //Relevant context information about the request.


    @JsonCreator
    private OriginalRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context) {

        Assert.notNull(userId, "User cannot be null");
        Assert.notNull(resourceId, "Resource cannot be null");
        Assert.notNull(context, "Context cannot be null");

        this.userId = userId;
        this.resourceId = resourceId;
        this.context = context;
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
        if (!(o instanceof OriginalRequest)) {
            return false;
        }
        OriginalRequest that = (OriginalRequest) o;
        return userId.equals(that.userId) &&
                resourceId.equals(that.resourceId) &&
                context.equals(that.context);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(userId, resourceId, context);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", OriginalRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of the OriginalRequest.  This is a variant of the Fluent Builder
     * which will use String or optionally JsonNodes for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * OriginalRequest class.
         *
         * @return interface  {@link IUser} for the next step in the build.
         */
        public static IUser create() {
            return user -> resource -> context ->
                    new OriginalRequest(user, resource, context);
        }

        /**
         * Adds the user ID information to the message.
         */
        interface IUser {
            /**
             * Adds the user's ID.
             *
             * @param userId user ID for the request.
             * @return interface  {@link IResource} for the next step in the build.
             */
            IResource withUser(String userId);
        }

        /**
         * Adds the resource ID information to the message.
         */
        interface IResource {
            /**
             * Adds the resource ID.
             *
             * @param resourceId resource ID for the request.
             * @return interface {@link IContext} for the next step in the build.
             */
            IContext withResource(String resourceId);
        }

        /**
         * Adds the user context information to the message.
         */
        interface IContext {
            /**
             * Adds the user context information.
             *
             * @param context information about this request.
             * @return class {@link OriginalRequest} this builder is set-up to create.
             */
            default OriginalRequest withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information. Uses a JsonNode string form of the information.
             *
             * @param context information about this request.
             * @return class {@link OriginalRequest} for the completion of the builder steps is to create the class.
             */
            OriginalRequest withContextNode(JsonNode context);
        }

    }
}


