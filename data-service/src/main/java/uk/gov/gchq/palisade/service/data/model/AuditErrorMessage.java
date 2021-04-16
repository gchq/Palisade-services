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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents information for an error that has occurred during the processing of a request. This information is
 * forwarded to the Audit Service. Note all of the services can potentially send an {@code AuditErrorMessage}.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class AuditErrorMessage extends AuditMessage {

    private final Throwable error;  //Error that occurred

    @JsonCreator
    private AuditErrorMessage(
            final @JsonProperty("leafResourceId") String leafResourceId,
            final @JsonProperty("userId") String userId,
            final @JsonProperty("resourceId") String resourceId,
            final @JsonProperty("context") Context context,
            final @JsonProperty("attributes") Map<String, Object> attributes,
            final @JsonProperty("error") Throwable error) {

        super(userId, resourceId, context, attributes, leafResourceId);

        this.error = Optional.ofNullable(error).orElseThrow(() -> new IllegalArgumentException("Error" + " cannot be null"));

    }

    @Generated
    public Throwable getError() {
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
         * @return interface {@link ILeafResourceId} for the next step in the build.
         */
        public static ILeafResourceId create() {
            return leafResourceId -> userId -> resourceId -> context -> attributes -> error ->
                    new AuditErrorMessage(leafResourceId, userId, resourceId, context, attributes, error);
        }

        /**
         * Starter method for the Builder class that uses the {@link AuditableAuthorisedDataRequest} objects for the
         * construction and will expect the next to builder method to be for the attribute map
         *
         * @param auditableAuthorisedDataRequest information from the authorising data request
         * @return interface {@link IAttributes} for the next step in the build.
         */

        public static IAttributes create(final AuditableAuthorisedDataRequest auditableAuthorisedDataRequest) {
            DataRequest dataRequest = auditableAuthorisedDataRequest.getDataRequest();
            AuthorisedDataRequest authorisedDataRequest = auditableAuthorisedDataRequest.getAuthorisedDataRequest();

            return create()
                    .withLeafResourceId(dataRequest.getLeafResourceId())
                    .withUserId(authorisedDataRequest.getUser().getUserId().getId())
                    .withResourceId(authorisedDataRequest.getResource().getId())
                    .withContext(authorisedDataRequest.getContext());
        }

        /**
         * Starter method for the Builder class that uses the {@link DataRequest} and will expect the next builder
         * method to be for the attribute map
         *
         * @param dataRequest the authorised request stored by the Attribute Masking Service
         * @return interface {@link IAttributes} for the next step in the build.
         */

        public static IAttributes create(final DataRequest dataRequest) {
            return create()
                    .withLeafResourceId(dataRequest.getLeafResourceId())
                    .withUserId(null)
                    .withResourceId(null)
                    .withContext(null);

        }

        /**
         * Adds the Leaf Resource Id for this request to the message.
         */
        public interface ILeafResourceId {
            /**
             * Adds the leafResource Id.
             *
             * @param leafResourceId leaf resource id for this  request.
             * @return interface {@link IUserId} for the next step of the build.
             */
            IUserId withLeafResourceId(String leafResourceId);
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
