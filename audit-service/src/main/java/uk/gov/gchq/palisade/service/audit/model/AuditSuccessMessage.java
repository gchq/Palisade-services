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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents information for a successful processing of a request which is forwarded to the Audit Service.
 * Note there are three classes that effectively represent the same kind of data but represent a different
 * stage of the process:
 * uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage is the message received by the Audit Service.
 * uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage is the message sent by the Filtered Resource Service.
 * uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage is the message sent by the Data Service.
 */
public final class AuditSuccessMessage extends AuditMessage {

    private final String leafResourceId;  //leafResource ID for the resource

    @JsonCreator
    private AuditSuccessMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("serviceName") String serviceName,
            final @JsonProperty("timestamp") String timestamp,
            final @JsonProperty("serverIP") String serverIP,
            final @JsonProperty("serverHostname") String serverHostname,
            final @JsonProperty("attributes") JsonNode attributes,
            final @JsonProperty("leafResourceId") String leafResourceId) {

        super(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes);
        this.leafResourceId = Optional.ofNullable(leafResourceId).orElseThrow(() -> new IllegalArgumentException("Resource ID cannot be null"));
    }

    /**
     * Returns leaf resource ID
     *
     * @return leaf resource ID
     */
    @Generated
    public String getLeafResourceId() {
        return leafResourceId;
    }

    /**
     * Builder class for the creation of instances of the AuditSuccessMessage. This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * AuditSuccessMessage class.
         *
         * @return public interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> serviceName -> timestamp -> serverIP -> serverHostname -> attributes -> leafResource ->
                    new AuditSuccessMessage(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes, leafResource);
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
         * Adds the context information to the message.
         */
        public interface IContext {
            /**
             * Adds the context information.
             *
             * @param context the reason why the user wants access to the data.
             * @return public interface {@link IServiceName} for the next step in the build.
             */
            default IServiceName withContext(final Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the context information. Uses a JsonNode string form of the information.
             *
             * @param context the reason why the user wants access to the data.
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
             * @param serviceName name of the service that created the message.
             * @return public interface {@link ITimestamp} for the next step in the build.
             */
            ITimestamp withServiceName(String serviceName);
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
             * @return public interface {@link IAttributes} for the next step in the build.
             */
            IAttributes withServerHostname(String serverHostname);
        }

        /**
         * Adds the attributes for the message.
         */
        public interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes the attributes for the request.
             * @return public interface {@link ILeafResourceId} for the next step in the build.
             */
            default ILeafResourceId withAttributes(final Map<String, Object> attributes) {
                return withAttributesNode(MAPPER.valueToTree(attributes));
            }

            /**
             * Adds the attributes for the message. Uses a JsonNode string form of the information.
             *
             * @param attributes the attributes for the request.
             * @return public interface {@link ILeafResourceId} for the next step in the build.
             */
            ILeafResourceId withAttributesNode(JsonNode attributes);
        }

        /**
         * Adds the leaf resource ID for the message.
         */
        public interface ILeafResourceId {
            /**
             * Adds the leaf resource ID for the message.
             *
             * @param leafResourceId leaf resource ID.
             * @return class {@link AuditSuccessMessage} for the completed class from the builder.
             */
            AuditSuccessMessage withLeafResourceId(String leafResourceId);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditSuccessMessage)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AuditSuccessMessage that = (AuditSuccessMessage) o;
        return leafResourceId.equals(that.leafResourceId);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), leafResourceId);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditSuccessMessage.class.getSimpleName() + "[", "]")
                .add("leafResourceId='" + leafResourceId + "'")
                .add(super.toString())
                .toString();
    }
}
