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


import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * The class contains the authorised access to the resource.  This class is a container for the original data,
 * {@link DataRequest}, the reference to the data that is authorised to be seen, {@link AuthorisedData} and
 * {@link AuditErrorMessage} processing.  The container will hold the {@code AuditErrorMessage} if there was an error
 * in getting the authorised data.
 */
public final class AuditableAuthorisedDataRequest {

    private final DataRequest dataRequest;
    private final AuthorisedData authorisedData;
    private final AuditErrorMessage auditErrorMessage;

    private AuditableAuthorisedDataRequest(
            final DataRequest dataRequest,
            final AuthorisedData authorisedData,
            final AuditErrorMessage auditErrorMessage) {

        this.dataRequest = dataRequest;
        this.authorisedData = authorisedData;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public DataRequest getDataRequest() {
        return dataRequest;
    }

    @Generated
    public AuthorisedData getAuthorisedData() {
        return authorisedData;
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
             * Adds the dataRequest.
             *
             * @param dataRequest initial request data
             * @return interface {@link IDataResponse} for the next step of the build
             */
            IDataResponse withDataRequest(DataRequest dataRequest);

        }

        /**
         * Compose wityh either {@link AuthorisedData} or {@link AuditErrorMessage} to create the
         * AuditableAuthorisedDataRequest
         */
        public interface IDataResponse {
            /**
             * Adds the data from the
             *
             * @param authorisedData for the resources
             * @return class {@link AuditableAuthorisedDataRequest} for the final step in the build.
             */
            default AuditableAuthorisedDataRequest withAuthorisedData(AuthorisedData authorisedData) {
                return withAuthorisedDataAndError(authorisedData, null);
            }

            /**
             * Adds the data from the
             *
             * @param auditErrorMessage for the error that occurred
             * @return class {@link AuditableAuthorisedDataRequest} for the final step in the build.
             */
            default AuditableAuthorisedDataRequest withAuditErrorMessage(AuditErrorMessage auditErrorMessage) {
                return withAuthorisedDataAndError(null, auditErrorMessage);
            }

            /**
             * Adds both the {@link AuthorisedData } and the {@link AuditErrorMessage} for the creation of the object
             * @param authorisedData  value or null
             * @param auditErrorMessage value or null
             * @return value object
             */
            AuditableAuthorisedDataRequest withAuthorisedDataAndError(AuthorisedData authorisedData, AuditErrorMessage auditErrorMessage);

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
                Objects.equals(authorisedData, that.authorisedData) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(dataRequest, authorisedData, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditableAuthorisedDataRequest.class.getSimpleName() + "[", "]")
                .add("dataRequestModel= " + dataRequest)
                .add("authorisedData= " + authorisedData)
                .add("auditErrorMessage= " + auditErrorMessage)
                .toString();
    }
}