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

import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents information for a successful processing of a request which is forwarded to the audit-service.
 * Note there are three classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage is the message received by the Audit Service.
 * uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage is the message sent by the Filtered Resource Service.
 * uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage is the message sent by the Data Service.
 * The version produced by the Data Service is unique in that it includes the leafResourceId and the
 * two resource counters: records processed; and records returned; which are included in the attributes map.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AuditSuccessMessage extends AuditMessage {

    public static final String RECORDS_PROCESSED = "RECORDS_PROCESSED";
    public static final String RECORDS_RETURNED = "RECORDS_RETURNED";

    @JsonCreator
    private AuditSuccessMessage(
            final @JsonProperty("leafResourceId") String leafResourceId,
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") Context context,
            final @JsonProperty("attributes") Map<String, Object> attributes) {

        super(userId, resourceId, context, attributes, leafResourceId);
    }

    /**
     * Builder class for the creation of instances of the AuditSuccessMessage.  This is a variant of the Fluent Builder
     * which will use Java Objects for the components in the build.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * AuditSuccessMessage class.
         *
         * @return interface {@link IUserId} for the next step in the build.
         */
        public static ILeafResourceId create() {
            return leafResourceId ->  userId -> resourceId -> context -> attributes ->
                    new AuditSuccessMessage(leafResourceId,  userId, resourceId, context, attributes);
        }

        /**
         * Starter method for the Builder class that uses a DataRequest for the request specific part of the Audit message.
         * It is followed by the call to add resource with the {@code IAttributes} interface to create the
         * AuditSuccessMessage class.
         *
         * @param auditableAuthorisedDataRequest the authorised request stored by the attribute-masking-service
         * @return interface {@link IAttributes} for the next step in the build.
         */
        public static IAttributes create(final AuditableAuthorisedDataRequest auditableAuthorisedDataRequest) {
            DataRequest dataRequest = auditableAuthorisedDataRequest.getDataRequest();
            AuthorisedDataRequest readerRequestModel  = auditableAuthorisedDataRequest.getAuthorisedDataRequest();

            return create()
                    .withLeafResourceId(dataRequest.getLeafResourceId())
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
             * @return interface {@link IUserId} for the next step in the build.
             */
            IUserId withLeafResourceId(String leafResource);
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
             * Adds the attributes supplied by the data service that are included in the auditing message:
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

        return super.equals(o);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditSuccessMessage.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .toString();
    }
}
