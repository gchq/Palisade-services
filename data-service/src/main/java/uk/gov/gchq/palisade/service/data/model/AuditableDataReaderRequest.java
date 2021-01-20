/*
 * Copyright 2021 Crown Copyright
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
 * The class contains the authorised access to the resource.  This class is a container for the {@link DataRequestModel}
 * {@link DataReaderRequestModel} and {@link AuditErrorMessage} during stream processing.  The container will hold the
 * {@code AuditErrorMessage} when there has been an error in the process.
 */
public class AuditableDataReaderRequest {

    private final DataRequestModel dataRequestModel;
    private final DataReaderRequestModel dataReaderRequestModel;
    private final AuditErrorMessage auditErrorMessage;

    private AuditableDataReaderRequest(
            final DataRequestModel dataRequestModel,
            final DataReaderRequestModel dataReaderRequestModel,
            final AuditErrorMessage auditErrorMessage) {

        this.dataRequestModel = dataRequestModel;
        this.dataReaderRequestModel = dataReaderRequestModel;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public DataRequestModel getDataRequestModel() {
        return dataRequestModel;
    }

    @Generated
    public DataReaderRequestModel getDataReaderRequestModel() {
        return dataReaderRequestModel;
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
        public static IDataRequestModel create() {
            return dataRequest -> readerRequestModel -> error ->
                    new AuditableDataReaderRequest(dataRequest, readerRequestModel, error);
        }

        /**
         * Adds the {@link DataRequestModel} to the container
         */
        public interface IDataRequestModel {
            /**
             * Adds the dataRequest.
             *
             * @param dataRequest initial request data
             * @return interface {@link IDataReaderRequestModel} for the next step of the build
             */
            IDataReaderRequestModel withDataRequestModel(DataRequestModel dataRequest);

        }

        /**
         * Compose with {@link DataReaderRequestModel}
         */
        public interface IDataReaderRequestModel {
            /**
             * Adds the data from the
             *
             * @param dataReaderRequestModel or null
             * @return interface {@link IError} for the next step of the build.
             */
            IError withDataReaderRequestModel(DataReaderRequestModel dataReaderRequestModel);

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
            AuditableDataReaderRequest withErrorMessage(AuditErrorMessage auditErrorMessage);

        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditableDataReaderRequest)) {
            return false;
        }
        AuditableDataReaderRequest that = (AuditableDataReaderRequest) o;
        return dataRequestModel.equals(that.dataRequestModel) &&
                Objects.equals(dataReaderRequestModel, that.dataReaderRequestModel) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(dataRequestModel, dataReaderRequestModel, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditableDataReaderRequest.class.getSimpleName() + "[", "]")
                .add("dataRequestModel=                " + dataRequestModel)
                .add("dataReaderRequestModel=                " + dataReaderRequestModel)
                .add("auditErrorMessage=                " + auditErrorMessage)
                .toString();
    }
}
