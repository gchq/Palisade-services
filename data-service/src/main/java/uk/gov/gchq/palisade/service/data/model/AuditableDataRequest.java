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
 * The class contains the authorised access to the resource.  This class is a container for the {@link DataRequest}
 * {@link DataResponse} and {@link AuditErrorMessage} during stream processing.  The container will hold the
 * {@code AuditErrorMessage} when there has been an error in the process.
 */
public final class AuditableDataRequest {

    private final DataRequest dataRequest;
    private final DataResponse dataResponse;
    private final AuditErrorMessage auditErrorMessage;

    private AuditableDataRequest(
            final DataRequest dataRequest,
            final DataResponse dataResponse,
            final AuditErrorMessage auditErrorMessage) {

        this.dataRequest = dataRequest;
        this.dataResponse = dataResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public DataRequest getDataRequest() {
        return dataRequest;
    }

    @Generated
    public DataResponse getDataResponse() {
        return dataResponse;
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
            return dataRequest -> dataResponse -> error ->
                    new AuditableDataRequest(dataRequest, dataResponse, error);
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
         * Compose with {@link DataResponse}
         */
        public interface IDataResponse {
            /**
             * Adds the data from the
             *
             * @param dataResponse or null
             * @return interface {@link IError} for the next step of the build.
             */
            IError withDataResponse(DataResponse dataResponse);

        }

        /**
         * Adds the {@code AuditErrorMessage}
         */
        public interface IError {
            /**
             * Adds the error message.
             *
             * @param auditErrorMessage or null
             * @return interface {@link IError} for the completed class from the builder.
             */
            AuditableDataRequest withErrorMessage(AuditErrorMessage auditErrorMessage);

        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditableDataRequest)) {
            return false;
        }
        AuditableDataRequest that = (AuditableDataRequest) o;
        return dataRequest.equals(that.dataRequest) &&
                Objects.equals(dataResponse, that.dataResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(dataRequest, dataResponse, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditableDataRequest.class.getSimpleName() + "[", "]")
                .add("dataRequestModel=                " + dataRequest)
                .add("dataResponse=                " + dataResponse)
                .add("auditErrorMessage=                " + auditErrorMessage)
                .toString();
    }
}