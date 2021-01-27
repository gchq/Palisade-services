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

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.DataResponse;

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
    private final DataService dataService;

    /**
     * AuditableDataService constructor
     *
     * @param dataService the current Data Service implemetation
     */
    public AuditableDataService(final DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Provides a wrapped message with the reference to the resources that are to be provided to the client or an
     * error message
     *
     * @param dataRequest request information from the client
     * @return reference to the resource information or error message
     */
    public CompletableFuture<AuditableDataRequest> authoriseRequest(final DataRequest dataRequest) {
        return dataService.authoriseRequest(dataRequest)
                .thenApply(dataResponse -> AuditableDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withDataResponse(dataResponse)
                        .withErrorMessage(null))
                .exceptionally(e -> AuditableDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withDataResponse(null)
                        .withErrorMessage(AuditErrorMessage.Builder.create(dataRequest)
                                .withAttributes(Collections.singletonMap("method", "authoriseRequest"))
                                .withError(e)));
    }

    /**
     * Provides an {@link OutputStream}
     *
     * @param auditableDataRequest saf
     * @param outputStream         asdf
     * @return asd
     */
    public CompletableFuture<AuditableDataResponse> read(final AuditableDataRequest auditableDataRequest, final OutputStream outputStream) {
        DataRequest dataRequest = auditableDataRequest.getDataRequest();
        DataResponse dataResponse = auditableDataRequest.getDataResponse();
        AtomicLong recordsProcessed = new AtomicLong(0);
        AtomicLong recordsReturned = new AtomicLong(0);

        return dataService.read(dataResponse, outputStream, recordsProcessed, recordsReturned)
                .thenApply(pair -> AuditableDataResponse.Builder.create()
                        .withToken(dataRequest.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableDataRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withAuditErrorMessage(null))
                .exceptionally(e -> AuditableDataResponse.Builder.create()
                        .withToken(dataRequest.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableDataRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(auditableDataRequest)
                                .withAttributes(Collections.singletonMap("method", "read"))
                                .withError(e)));
    }
}
