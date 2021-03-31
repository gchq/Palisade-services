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
package uk.gov.gchq.palisade.service.audit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents information for an error that has occurred during the processing of a request. This information is
 * received by the audit-service and processed.
 * Note each of the services can potentially send an error message.  This version is for recording the information in
 * the audit service.
 */
public final class AuditErrorMessage extends AuditMessage {

    private final JsonNode error;  //Error that occurred

    @JsonCreator
    private AuditErrorMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("serviceName") String serviceName,
            final @JsonProperty("timestamp") String timestamp,
            final @JsonProperty("serverIP") String serverIP,
            final @JsonProperty("serverHostname") String serverHostname,
            final @JsonProperty("attributes") JsonNode attributes,
            final @JsonProperty("error") JsonNode error) {

        super(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes);
        this.error = Optional.ofNullable(error).orElseThrow(() -> new IllegalArgumentException("Error cannot be null"));
    }

    /**
     * Returns the error node which contains the tree model of the error
     * ({@code Throwable}).
     *
     * @return the error node of the {@code Throwable}
     */
    @JsonIgnore
    @Generated
    public JsonNode getErrorNode() {
        return error;
    }

    /**
     * Builder class for the creation of instances of the AuditErrorMessage.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditErrorMessage class.
         *
         * @return public interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> serviceName -> timestamp -> serverIP -> serverHostname -> attributes -> error ->
                    new AuditErrorMessage(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes, error);
        }

        /**
         * Adds the user ID information to the message.
         */
        public interface IUserId {
            /**
             * Adds the user ID.
             *
             * @param userId user ID for the request.
             * @return public interface {@link IResourceId} for the next step in the build.
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
             * @return public interface {@link IContext} for the next step in the build.
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
             * @return public interface {@link IServiceName} for the next step in the build.
             */
            default IServiceName withContext(final Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             * @return public interface {@link IServiceName} for the next step in the build.
             */
            IServiceName withContextNode(JsonNode context);
        }

        /**
         * Adds the service name for the service that created this message.
         */
        public interface IServiceName {

            /**
             * Adds the service name.
             *
             * @param servicename name of the service that created the message.
             * @return public interface  {@link ITimestamp} for the next step in the build.
             */
            ITimestamp withServiceName(String servicename);
        }

        /**
         * Adds the timestamp for when the service created this message.
         */
        public interface ITimestamp {

            /**
             * Adds the timestamp for the message.
             *
             * @param timestamp timestamp for the request.
             * @return public interface {@link IServerIp} for the next step in the build.
             */
            IServerIp withTimestamp(String timestamp);
        }

        /**
         * Adds the server IP information for the se.
         */
        public interface IServerIp {

            /**
             * Adds the server IP information for the message.
             *
             * @param serverIp where the message was created.
             * @return public interface  {@link IServerHostname} for the next step in the build.
             */
            IServerHostname withServerIp(String serverIp);
        }

        /**
         * Adds the server host name for the message.
         */
        public interface IServerHostname {
            /**
             * Adds the server host name for where the message was created.
             *
             * @param serverHostname server host name.
             * @return public interface  {@link IAttributes} for the next step in the build.
             */
            IAttributes withServerHostname(String serverHostname);
        }

        /**
         * Adds the server host name for the message.
         */
        public interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes timestamp for the request.
             * @return public interface {@link IError} for the next step in the build.
             */
            default IError withAttributes(final Map<String, Object> attributes) {
                return withAttributesNode(MAPPER.valueToTree(attributes));
            }

            /**
             * Adds the attributes for the message.  Uses a JsonNode string form of the information.
             *
             * @param attributes user context for the request.
             * @return public interface {@link IError} for the next step in the build.
             */
            IError withAttributesNode(JsonNode attributes);
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
            default AuditErrorMessage withError(final Throwable error) {
                return withErrorNode(MAPPER.valueToTree(error));
            }

            /**
             * Adds the attributes for the message.  Uses a JsonNode string form of the information.
             *
             * @param error user context for the request.
             * @return class  {@link AuditErrorMessage} for the completed class from the builder.
             */
            AuditErrorMessage withErrorNode(JsonNode error);
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
        return error.equals(that.error);
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

