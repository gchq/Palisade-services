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
package uk.gov.gchq.palisade.service.data.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents information for a successful processing of a request which is forwarded to the audit-service.
 * Note there are three classes that effectively represent the same data but represent a different stage of the process.
 * uk.gov.gchq.palisade.service.audit.request.AuditSuccessMessage is the message received by the Audit Service.
 * uk.gov.gchq.palisade.service.results.request.AuditSuccessMessage is the message sent by the results-service.
 * uk.gov.gchq.palisade.service.data.request.AuditSuccessMessage is the message sent by the data-service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class AuditSuccessMessage extends AuditMessage {

    public static final String RECORDS_PROCESSED = "RECORDS_PROCESSED";
    public static final String RECORDS_RETURNED = "RECORDS_RETURNED";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("leafResourceId")
    private final String leafResourceId;  //leafResource ID for the resource

    @JsonCreator
    private AuditSuccessMessage(
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") JsonNode context,
            final @JsonProperty("leafResourceId") String leafResourceId,
            final @JsonProperty("attributes") Map<String, Object> attributes) {

        super(userId, resourceId, context, attributes);
        this.leafResourceId = Optional.ofNullable(leafResourceId).orElseThrow(() -> new RuntimeException("Resource ID cannot be null"));
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
            return userId -> resourceId -> context -> leafResource -> attributes ->
                    new AuditSuccessMessage(userId, resourceId, context, leafResource, attributes);
        }

        /**
         * Starter method for the Builder class that uses a DataRequest for the request specific part of the Audit message.
         * This method is called followed by the call to add resource with the IResource interface to create the
         * AuditSuccessMessage class. The service specific information is generated in the parent class, AuditMessage.
         *
         * @return interface {@link ILeafResourceId} for the next step in the build.
         */
        public static IAttributes create(final AuthorisedRequestEntity requestEntity) {
            return create()
                    .withUserId(requestEntity.getUser().getUserId().getId())
                    .withResourceId(requestEntity.getResourceId())
                    .withContext(requestEntity.getContext())
                    .withLeafResourceId(requestEntity.getLeafResource().getId());
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
             */
            default ILeafResourceId withContext(Context context) {
                return withContextNode(MAPPER.valueToTree(context));
            }

            /**
             * Adds the user context information.  Uses a JsonNode string form of the information.
             *
             * @param context user context for the request.
             */
            ILeafResourceId withContextNode(JsonNode context);
        }

        /**
         * Adds the leaf resource ID for the message.
         */
        public interface ILeafResourceId {
            /**
             * Adds the leaf resource ID for the message.
             *
             * @param leafResource leaf resource ID.
             */
            IAttributes withLeafResourceId(String leafResource);
        }

        /**
         * Adds the attributes for the message.
         */
        public interface IAttributes {
            /**
             * Adds the attributes for the message.
             *
             * @param attributes timestamp for the request.
             */
            AuditSuccessMessage withAttributes(Map<String, Object> attributes);

            default AuditSuccessMessage withRecordsProcessedAndReturned(final Long recordsProcessed, final Long recordsReturned) {
                return withAttributes(Map.of(
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

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditSuccessMessage.class.getSimpleName() + "[", "]")
                .add("leafResourceId='" + leafResourceId + "'")
                .add(super.toString())
                .toString();
    }
}
