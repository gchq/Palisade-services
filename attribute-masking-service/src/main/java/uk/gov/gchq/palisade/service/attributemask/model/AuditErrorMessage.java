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
import com.fasterxml.jackson.databind.JsonNode;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents information for an error that has occurred during the processing of a request.
 * This information is forwarded to the Audit Service.
 * Note all the services can potentially send an error message.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class AuditErrorMessage extends AuditMessage {

    private final Throwable error;  //Error that occurred

    @JsonCreator
    private AuditErrorMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("attributes") Map<String, Object> attributes,
            final @JsonProperty("error") Throwable error) {
        super(userId, resourceId, context, attributes);
        this.error = Optional.ofNullable(error).orElseThrow(() -> new IllegalArgumentException("error cannot be null"));

    }

    @Generated
    public Throwable getError() {
        return error;
    }

    /**
     * Builder class for the creation of instances of the AuditSuccessMessage.
     * This is a variant of the Fluent Builder which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditSuccessMessage class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> attributes -> error ->
                    new AuditErrorMessage(userId, resourceId, context, attributes, error);
        }

        /**
         * Starter method for the Builder class that uses a AttributeMaskingRequest for the request specific part of the Audit message.
         * This method is called followed by the call to add resource with the IResource interface to create the
         * AuditErrorMessage class. The service specific information is generated in the parent class, AuditMessage.
         *
         * @param request    the request message that was sent to the palisade-service
         * @param attributes optional information stored in a Map
         * @return interface {@link IError} for the next step in the build.
         */
        public static IError create(final AttributeMaskingRequest request, final Map<String, Object> attributes) {
            return create()
                    .withUserId(request.getUserId())
                    .withResourceId(request.getResourceId())
                    .withContextNode(request.getContextNode())
                    .withAttributes(attributes);
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
             * @return interface {@link IAttributes} for the next step in the build.
             */
            default IAttributes withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             * @return interface {@link IAttributes} for the next step in the build.
             */
            IAttributes withContextNode(JsonNode context);
        }

        /**
         * Adds the attributes for the message.
         */
        public interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes timestamp for the request.
             * @return interface {@link IError} for the next step in the build.
             */
            IError withAttributes(Map<String, Object> attributes);
        }

        /**
         * Adds the error that occurred.
         */
        public interface IError {
            /**
             * Adds the error for the message.
             *
             * @param error that occurred.
             * @return class  {@link AuditErrorMessage} for the completed class from the builder.
             */
            AuditErrorMessage withError(Throwable error);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditErrorMessage)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AuditErrorMessage that = (AuditErrorMessage) o;
        return error.getMessage().equals(that.error.getMessage());
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), error);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditErrorMessage.class.getSimpleName() + "[", "]")
                .add("error=" + error)
                .add(super.toString())
                .toString();
    }
}
