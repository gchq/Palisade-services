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
package uk.gov.gchq.palisade.service.data.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;
import uk.gov.gchq.palisade.service.data.model.DataReaderRequestModel;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;

import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides an auditable wrapper to the {@link DataService}.  For each of the methods provided in the in the
 * {@code DataService}, there is a corresponding method in this class for requesting the information and providing a
 * response wrapped with the data or the exception when an error has occurred.
 */
public class AuditableDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditableDataService.class);

    private final DataService dataService;

    public AuditableDataService(final DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Provides a wrapped message with the reference to the resources that are to be provided to the client or an
     * error message
     * @param dataRequestModel request information from the client
     * @return reference to the resource information or error message
     */
    public CompletableFuture<AuditableDataReaderRequest> authoriseRequest(final DataRequestModel dataRequestModel) {
        return dataService.authoriseRequest(dataRequestModel)
                .thenApply(dataReaderRequest -> AuditableDataReaderRequest.Builder.create()
                        .withDataRequestModel(dataRequestModel)
                        .withDataReaderRequestModel(dataReaderRequest)
                        .withErrorMessage(null))
                .exceptionally(e -> AuditableDataReaderRequest.Builder.create()
                        .withDataRequestModel(dataRequestModel)
                        .withDataReaderRequestModel(null)
                        .withErrorMessage(AuditErrorMessage.Builder.create(dataRequestModel)
                                .withAttributes(Collections.singletonMap("method", "authoriseRequest"))
                                .withError(e)));
    }

    /**
     * Provides an {@link OutputStream}
     * @param auditableDataReaderRequest saf
     * @param outputStream asdf
     * @return asd
     */
    public CompletableFuture<AuditableDataReaderResponse> read(final AuditableDataReaderRequest auditableDataReaderRequest, final OutputStream outputStream) {
        DataRequestModel dataRequestModel = auditableDataReaderRequest.getDataRequestModel();
        DataReaderRequestModel dataReaderRequestModel = auditableDataReaderRequest.getDataReaderRequestModel();
        AtomicLong recordsProcessed = new AtomicLong(0);
        AtomicLong recordsReturned = new AtomicLong(0);

        return dataService.read(dataReaderRequestModel, outputStream, recordsProcessed, recordsReturned)
                .thenApply(pair -> AuditableDataReaderResponse.Builder.create()
                        .withToken(dataRequestModel.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableDataReaderRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withAuditErrorMessage(null))
                .exceptionally(e -> AuditableDataReaderResponse.Builder.create()
                        .withToken(dataRequestModel.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableDataReaderRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(auditableDataReaderRequest)
                                .withAttributes(Collections.singletonMap("method", "read"))
                                .withError(e)));
    }
}
