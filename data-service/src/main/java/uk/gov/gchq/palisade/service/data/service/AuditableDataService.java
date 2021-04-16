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

import uk.gov.gchq.palisade.service.data.common.data.DataService;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.ExceptionSource;

import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides an auditable wrapper to the {@link DataService}. For each of the methods provided in the in the
 * {@code DataService}, there is a corresponding method in this class for requesting the information and providing a
 * response wrapped with the data or the exception when an error has occurred.
 */
public class AuditableDataService {
    private final DataService dataService;

    /**
     * AuditableDataService constructor
     *
     * @param dataService the current Data Service implementation
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
    public CompletableFuture<AuditableAuthorisedDataRequest> authoriseRequest(final DataRequest dataRequest) {
        return dataService.authoriseRequest(dataRequest)
                .thenApply(authorisedDataRequest -> AuditableAuthorisedDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withAuthorisedData(authorisedDataRequest))
                .exceptionally(e -> AuditableAuthorisedDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(dataRequest)
                                .withAttributes(Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.AUTHORISED_REQUEST))
                                .withError(e)));
    }

    /**
     * Reads the authorised resource and passes this onto the client in the form an {@link OutputStream}. The response
     * is used in the construction of the audit message for this request.
     *
     * @param auditableAuthorisedDataRequest provides the reference to the authorised data request
     * @param outputStream                   is used to provide the requested data to be forwarded to the client
     * @return information on the resources that have been provided
     */
    public CompletableFuture<AuditableDataResponse> read(final AuditableAuthorisedDataRequest auditableAuthorisedDataRequest, final OutputStream outputStream) {
        DataRequest dataRequest = auditableAuthorisedDataRequest.getDataRequest();
        AuthorisedDataRequest authorisedDataRequest = auditableAuthorisedDataRequest.getAuthorisedDataRequest();
        AtomicLong recordsProcessed = new AtomicLong(0);
        AtomicLong recordsReturned = new AtomicLong(0);

        return dataService.read(authorisedDataRequest, outputStream, recordsProcessed, recordsReturned)
                .thenApply(success -> AuditableDataResponse.Builder.create()
                        .withToken(dataRequest.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableAuthorisedDataRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withoutAuditErrorMessage())
                .exceptionally(e -> AuditableDataResponse.Builder.create()
                        .withToken(dataRequest.getToken())
                        .withSuccessMessage(AuditSuccessMessage.Builder.create(auditableAuthorisedDataRequest)
                                .withRecordsProcessedAndReturned(recordsProcessed.get(), recordsReturned.get()))
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(auditableAuthorisedDataRequest)
                                .withAttributes(Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.READ))
                                .withError(e)));
    }
}
