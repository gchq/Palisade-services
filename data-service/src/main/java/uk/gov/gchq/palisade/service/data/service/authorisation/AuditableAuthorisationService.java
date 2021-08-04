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
package uk.gov.gchq.palisade.service.data.service.authorisation;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.ExceptionSource;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Provides an auditable wrapper to the {@link AuthorisationService}. For each of the methods provided in the in the
 * {@link AuthorisationService}, there is a corresponding method in this class for requesting the information and providing a
 * response wrapped with the data or the exception when an error has occurred.
 */
public class AuditableAuthorisationService {
    private final AuthorisationService authorisationService;

    /**
     * AuditableDataService constructor
     *
     * @param authorisationService the current Data Service implementation
     */
    public AuditableAuthorisationService(final AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }

    /**
     * Provides a wrapped message with the reference to the resources that are to be provided to the client or an
     * error message
     *
     * @param dataRequest request information from the client
     * @return reference to the resource information or error message
     */
    public CompletableFuture<AuditableAuthorisedDataRequest> authoriseRequest(final DataRequest dataRequest) {
        return authorisationService.authoriseRequest(dataRequest)
                .thenApply(authorisedDataRequest -> AuditableAuthorisedDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withAuthorisedData(authorisedDataRequest))
                .exceptionally(e -> AuditableAuthorisedDataRequest.Builder.create()
                        .withDataRequest(dataRequest)
                        .withAuditErrorMessage(AuditErrorMessage.Builder.create(dataRequest)
                                .withAttributes(Collections.singletonMap(ExceptionSource.ATTRIBUTE_KEY, ExceptionSource.AUTHORISED_REQUEST))
                                .withError(e)));
    }
}
