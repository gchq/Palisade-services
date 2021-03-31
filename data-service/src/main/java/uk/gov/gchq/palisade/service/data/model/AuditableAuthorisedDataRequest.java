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

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * The class contains the authorised access to the resource.  This class is a container for the original data,
 * {@link DataRequest}, the reference to the data that is authorised to be seen, {@link AuthorisedDataRequest} and
 * {@link AuditErrorMessage} processing.  The container will hold the {@code AuditErrorMessage} if there was an error
 * in getting the authorised data.
 */
public final class AuditableAuthorisedDataRequest {

    private final DataRequest dataRequest;
    private final AuthorisedDataRequest authorisedDataRequest;
    private final AuditErrorMessage auditErrorMessage;

    private AuditableAuthorisedDataRequest(
            final DataRequest dataRequest,
            final AuthorisedDataRequest authorisedDataRequest,
            final AuditErrorMessage auditErrorMessage) {

        this.dataRequest = dataRequest;
        this.authorisedDataRequest = authorisedDataRequest;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public DataRequest getDataRequest() {
        return dataRequest;
    }

    @Generated
    public AuthorisedDataRequest getAuthorisedDataRequest() {
        return authorisedDataRequest;
    }


    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    /**
     * The static builder
     */
    public static class Builder {

        /**
         * The starter method for the Builder class.
         *
         * @return the composed immutable object
         */
        public static IDataRequest create() {
            return dataRequest -> (authorisedData, error) -> new AuditableAuthorisedDataRequest(dataRequest, authorisedData, error);
        }

        /**
         * Adds the {@link DataRequest} to the container
         */
        public interface IDataRequest {
            /**
             * Adds the dataRequest to the message
             *
             * @param dataRequest initial request data
             * @return interface {@link IDataResponse} for the next step of the build
             */
            IDataResponse withDataRequest(DataRequest dataRequest);

        }

        /**
         * Compose with either {@link AuthorisedDataRequest} or {@link AuditErrorMessage} to create the
         * AuditableAuthorisedDataRequest
         */
        public interface IDataResponse {
            /**
             * Adds the authorised request and assigns the error message to null
             *
             * @param authorisedDataRequest for the resources
             * @return class {@link AuditableAuthorisedDataRequest} for the final step in the build.
             */
            default AuditableAuthorisedDataRequest withAuthorisedData(AuthorisedDataRequest authorisedDataRequest) {
                return withAuthorisedDataAndError(authorisedDataRequest, null);
            }

            /**
             * Adds the only the error message and assigns the authorised data to null
             *
             * @param auditErrorMessage for the error that occurred
             * @return class {@link AuditableAuthorisedDataRequest} for the final step in the build.
             */
            default AuditableAuthorisedDataRequest withAuditErrorMessage(AuditErrorMessage auditErrorMessage) {
                return withAuthorisedDataAndError(null, auditErrorMessage);
            }

            /**
             * Adds both the {@link AuthorisedDataRequest } and the {@link AuditErrorMessage} for the creation of the object
             *
             * @param authorisedDataRequest adds either the {@link AuthorisedDataRequest} or null to the builder
             * @param auditErrorMessage     adds either the {@link AuditErrorMessage} or null to the builder
             * @return class {@link AuditableAuthorisedDataRequest} for the completed class from the builder.
             */
            AuditableAuthorisedDataRequest withAuthorisedDataAndError(AuthorisedDataRequest authorisedDataRequest, AuditErrorMessage auditErrorMessage);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditableAuthorisedDataRequest)) {
            return false;
        }
        AuditableAuthorisedDataRequest that = (AuditableAuthorisedDataRequest) o;
        return dataRequest.equals(that.dataRequest) &&
                Objects.equals(authorisedDataRequest, that.authorisedDataRequest) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(dataRequest, authorisedDataRequest, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditableAuthorisedDataRequest.class.getSimpleName() + "[", "]")
                .add("dataRequestModel=" + dataRequest)
                .add("authorisedData=" + authorisedDataRequest)
                .add("auditErrorMessage=" + auditErrorMessage)
                .toString();
    }
}
