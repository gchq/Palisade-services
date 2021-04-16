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
package uk.gov.gchq.palisade.service.palisade.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import uk.gov.gchq.palisade.service.palisade.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents the original data that has been sent from the client to Palisade Service for a request to access data.
 * This data will be forwarded to a set of services with each contributing to the processing of this request.
 * This version represents the original request.
 * Next in the sequence is the input for User Service where this data will be used as a request for a User.
 * Note there are three classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest is the client request that has come into the Palisade Service.
 * uk.gov.gchq.palisade.service.palisade.model.PalisadeSystemResponse is the response from the Palisade Service which is sent to the User Service
 * uk.gov.gchq.palisade.service.user.request.UserRequest is the object received by the User Service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class PalisadeClientRequest {

    private final String userId;  //Unique identifier for the user.
    private final String resourceId;  //Resource that that is being asked to access.

    // Ignore class type on context object
    @JsonIgnoreProperties(ignoreUnknown = true)
    private final Map<String, String> context;

    @JsonCreator
    private PalisadeClientRequest(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") Map<String, String> context) {

        this.userId = Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID cannot be null"));
        this.resourceId = Optional.ofNullable(resourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID  cannot be null"));
        this.context = Optional.ofNullable(context).orElseThrow(() -> new IllegalArgumentException("Context cannot be null"));
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
    public Map<String, String> getContext() {
        return context;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PalisadeClientRequest)) {
            return false;
        }
        PalisadeClientRequest that = (PalisadeClientRequest) o;
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
        return new StringJoiner(", ", PalisadeClientRequest.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("resourceId='" + resourceId + "'")
                .add("context=" + context)
                .add(super.toString())
                .toString();
    }

    /**
     * Builder class for the creation of the PalisadeClientRequest. This is a variant of the Fluent Builder
     * which will use String or optionally JsonNodes for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * PalisadeClientRequest class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context ->
                    new PalisadeClientRequest(userId, resourceId, context);
        }

        /**
         * Adds the user ID information to the message.
         */
        public interface IUserId {
            /**
             * Adds the user's ID.
             *
             * @param userId user ID for the request.
             * @return interface  {@link IResourceId} for the next step in the build.
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
             * @param context information about this request.
             * @return class {@link PalisadeClientRequest} this builder is set-up to create.
             */
            PalisadeClientRequest withContext(Map<String, String> context);
        }
    }
}
