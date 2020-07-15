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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * Represents information for a successful processing of a request which is forwarded to the audit-service.
 * Note there are three classes that effectively represent the same kind of data but represent a different
 * stage of the process:
 * uk.gov.gchq.palisade.service.audit.request.AuditSuccessMessage is the message received by the Audit Service.
 * uk.gov.gchq.palisade.service.results.request.AuditSuccessMessage is the message sent by the results-service.
 * uk.gov.gchq.palisade.service.results.request.AuditSuccessMessage is the message sent by the data-service.
 */
public final class AuditSuccessMessage extends AuditMessage {

    @JsonProperty("leafResourceId")
    private final String leafResourceId;  //leafResource ID for the resource


    @JsonCreator
    private AuditSuccessMessage(

            final  String userId,
            final  String resourceId,
            final  JsonNode context,
            final  String serviceName,
            final  String timestamp,
            final  String serverIP,
            final  String serverHostname,
            final JsonNode attributes,
            final String leafResourceId
    ) {

        super(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes);

        Assert.notNull(leafResourceId, "Resource ID cannot be null");
        this.leafResourceId = leafResourceId;

    }


    @Generated
    public String getLeafResourceId() {
        return leafResourceId;
    }


    /**
     * Builder class for the creation of instances of the AuditSuccessMessage.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {


        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditSuccessMessage class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static IUserId create() {
            return userId -> resourceId -> context -> serviceName -> timestamp -> serverIP -> serverHostname -> attributes -> leafResource ->
                    new AuditSuccessMessage(userId, resourceId, context, serviceName, timestamp, serverIP, serverHostname, attributes, leafResource);
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
             * @return interface {@link ILeafResourceId} for the next step in the build.
             */
            default ILeafResourceId withAttributes(Map<String, Object> attributes) {
                return withAttributesNode(MAPPER.valueToTree(attributes));
            }

            /**
             * Adds the attributes for the message.  Uses a JsonNode string form of the information.
             *
             * @param attributes user context for the request.
             * @return interface {@link ILeafResourceId} for the next step in the build.
             */
            ILeafResourceId withAttributesNode(JsonNode attributes);

        }

        /**
         * Adds the leaf resource ID for the message.
         */
        interface ILeafResourceId {
            /**
             * Adds the leaf resource ID for the message.
             *
             * @param leafResourceId leaf resource ID.
             * @return class  {@link AuditSuccessMessage} for the completed class from the builder.
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
