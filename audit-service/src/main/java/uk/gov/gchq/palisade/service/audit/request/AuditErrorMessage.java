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
package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents information for an error that has occurred during the processing of a request. This information is
 * received by the audit-service and processed.
 * Note each of the services can potentially send an error message.  This version is for recording the information in
 * the audit service.
 */
public final class AuditErrorMessage extends AuditMessage {


    @JsonProperty("error")
    private final JsonNode error;  //Error that occurred


    @JsonCreator
    private AuditErrorMessage(

            final String userId,
            final String resourceId,
            final JsonNode context,
            final String serviceName,
            final String timestamp,
            final String serverIP,
            final String serverHostname,
            final JsonNode attributes,
            final JsonNode error) {

        super(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes);

        Assert.notNull(error, "Error cannot be null");
        this.error = error;

    }


    @Generated
    public Throwable getError() throws JsonProcessingException {
        return MAPPER.treeToValue(error, Throwable.class);
    }

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
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> serviceName -> timestamp -> serverIP -> serverHostname -> attributes -> error ->
                    new AuditErrorMessage(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes, error);
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
             * @return interface {@link IServiceName} for the next step in the build.
             */
            default IServiceName withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             * @return interface {@link IServiceName} for the next step in the build.
             */
            IServiceName withContextNode(JsonNode context);

        }

        /**
         * Adds the service name for the service that created this message.
         */
        interface IServiceName {

            /**
             * Adds the service name.
             *
             * @param servicename name of the service that created the message.
             * @return interface  {@link ITimeStamp} for the next step in the build.
             */
            ITimeStamp withServiceName(String servicename);
        }


        /**
         * Adds the timestamp for when the service created this message.
         */
        interface ITimeStamp {

            /**
             * Adds the timestamp for the message.
             *
             * @param timestamp timestamp for the request.
             * @return interface {@link IServerIp} for the next step in the build.
             */
            IServerIp withTimestamp(String timestamp);
        }

        /**
         * Adds the server IP information for the se.
         */
        interface IServerIp {

            /**
             * Adds the server IP information for the message.
             *
             * @param serverIp where the message was created.
             * @return interface  {@link IServerHostname} for the next step in the build.
             */
            IServerHostname withServerIp(String serverIp);
        }

        /**
         * Adds the server host name for the message.
         */
        interface IServerHostname {
            /**
             * Adds the server host name for where the message was created.
             *
             * @param serverHostname server host name.
             * @return interface  {@link IAttributes} for the next step in the build.
             */
            IAttributes withServerHostname(String serverHostname);
        }

        /**
         * Adds the server host name for the message.
         */
        interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes timestamp for the request.
             * @return interface {@link IError} for the next step in the build.
             */
            default IError withAttributes(Map<String, Object> attributes) {
                return withAttributesNode(MAPPER.valueToTree(attributes));
            }

            /**
             * Adds the attributes for the message.  Uses a JsonNode string form of the information.
             *
             * @param attributes user context for the request.
             * @return interface {@link IError} for the next step in the build.
             */
            IError withAttributesNode(JsonNode attributes);

        }

        /**
         * Adds the error that occurred.
         */
        interface IError {
            /**
             * Adds the error for the message.
             *
             * @param error that occurred.
             * @return class  {@link AuditErrorMessage} for the completed class from the builder.
             */
            default AuditErrorMessage withError(Throwable error) {
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

