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
package uk.gov.gchq.palisade.service.data.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents information for a successful processing of a request which is forwarded to the audit-service.
 * Note there are three classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.audit.request.AuditSuccessMessage is the message received by the Audit Service.
 * uk.gov.gchq.palisade.service.results.request.AuditSuccessMessage is the message sent by the results-service.
 * uk.gov.gchq.palisade.service.data.request.AuditSuccessMessage is the message sent by the data-service.
 * This one version is unique in that it includes the leafResourceId, token and the two counters for the number of
 * records process and records returned as part of the message.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AuditSuccessMessage extends AuditMessage {

    public static final String RECORDS_PROCESSED = "RECORDS_PROCESSED";
    public static final String RECORDS_RETURNED = "RECORDS_RETURNED";

    @JsonProperty("leafResourceId")
    private final String leafResourceId;  //leafResource ID for the resource

    @JsonCreator
    private AuditSuccessMessage(
            final @JsonProperty("leafResourceId") String leafResourceId,
            final @JsonProperty("token") String token,
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") Context context,
            final @JsonProperty("attributes") Map<String, Object> attributes) {

        super(leafResourceId, token, userId, resourceId, context, attributes);
        this.leafResourceId = Optional.ofNullable(leafResourceId).orElseThrow(() -> new RuntimeException("Resource ID cannot be null"));
    }

    @Override
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
         * AuditErrorMessage class.
         *
         * @return interface {@link AuditErrorMessage.Builder.IToken} for the next step in the build.
         */
        public static ILeafResourceId create() {
            return leafResourceId -> token -> userId -> resourceId -> context -> attributes ->
                    new AuditSuccessMessage(leafResourceId, token, userId, resourceId, context, attributes);
        }

        /**
         * Starter method for the Builder class that uses a DataRequest for the request specific part of the Audit message.
         * This method is called followed by the call to add resource with the IResource interface to create the
         * AuditSuccessMessage class. The service specific information is generated in the parent class, AuditMessage.
         *
         * @param auditableDataReaderRequest the authorised request stored by the attribute-masking-service
         * @return interface {@link IAttributes} for the next step in the build.
         */
        public static IAttributes create(final  AuditableDataReaderRequest auditableDataReaderRequest) {
            DataRequestModel dataRequestModel = auditableDataReaderRequest.getDataRequestModel();
            DataReaderRequestModel  readerRequestModel  = auditableDataReaderRequest.getDataReaderRequestModel();

            return create()
                    .withLeafResourceId(dataRequestModel.getLeafResourceId())
                    .withToken(dataRequestModel.getToken())
                    .withUserId(readerRequestModel.getUser().getUserId().getId())
                    .withResourceId(readerRequestModel.getResource().getId())
                    .withContext(readerRequestModel.getContext());
        }

        /**
         * Adds the leaf resource ID for the message.
         */
        public interface ILeafResourceId {
            /**
             * Adds the leaf resource ID for the message.
             *
             * @param leafResource leaf resource ID.
             * @return interface {@link IToken} for the next step in the build.
             */
            IToken withLeafResourceId(String leafResource);
        }


        /**
         * Adds the token information to the message.
         */
        public interface IToken {
            /**
             * Adds the token.
             *
             * @param token token to uniquely identify the request.
             * @return interface {@link IUserId} for the for the next step in the build.
             */
            IUserId withToken(String token);
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
             * @return interface {@link AuditSuccessMessage.Builder.ILeafResourceId} for the next step in the build.
             */
            IAttributes withContext(Context context);
        }

        /**
         * Adds the attributes for the message.
         */
        public interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes other attributes to add to the message object
             * @return completed {@link AuditSuccessMessage} object.
             */
            AuditSuccessMessage withAttributes(Map<String, Object> attributes);

            /**
             * Add the expected attributes supplied by the data service that require auditing,
             * the number of records processed (total number of records in the resource) and
             * the number of records returned (excludes those which were totally redacted, but
             * includes those that were just masked)
             *
             * @param recordsProcessed a count of the total number of records processed by the service
             * @param recordsReturned  a count of the number of records returned to the client (excludes
             *                         fully-redacted records)
             * @return completed {@link AuditSuccessMessage} object.
             */
            default AuditSuccessMessage withRecordsProcessedAndReturned(final Long recordsProcessed, final Long recordsReturned) {
                return withAttributes(
                        Map.of(
                                RECORDS_PROCESSED, recordsProcessed,
                                RECORDS_RETURNED, recordsReturned
                        ));
            }
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
}
