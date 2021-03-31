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
package uk.gov.gchq.palisade.service.data.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;

import java.io.OutputStream;
import java.util.Optional;

/**
 * Controller for Data Service. Provides the front end RESTFul web service to the client for retrieving resources that
 * have been registered with the Palisade Service. The request is in the form of information that will uniquely
 * identify the resource request and will return a data stream of the response data.
 */
@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final AuditableDataService auditableDataService;
    private final AuditMessageService auditMessageService;

    /**
     * Constructor for the DataController.
     *
     * @param auditableDataService service for retrieving data for the request
     * @param auditMessageService  service for sending audit success and error messages to the Audit Service
     */
    public DataController(final AuditableDataService auditableDataService, final AuditMessageService auditMessageService) {
        this.auditableDataService = auditableDataService;
        this.auditMessageService = auditMessageService;
    }

    /**
     * REST endpoint to read a resource request and return a streaming response.
     *
     * @param dataRequest the resource request
     * @return a streaming response holding the filtered data
     */
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(
            @RequestBody final DataRequest dataRequest) {
        LOGGER.info("Invoking read (chunked): {}", dataRequest);

        HttpStatus httpStatus = HttpStatus.OK;
        StreamingResponseBody stream = null;

        //first with the client information about the request, retrieve the authorised resource information
        AuditableAuthorisedDataRequest auditableAuthorisedDataRequest = auditableDataService.authoriseRequest(dataRequest).join();
        AuditErrorMessage authorisationErrorMessage = auditableAuthorisedDataRequest.getAuditErrorMessage();

        if (authorisationErrorMessage != null) {
            LOGGER.error("Error occurred processing the authoriseRequest : ", authorisationErrorMessage.getError());
            httpStatus = (HttpStatus.INTERNAL_SERVER_ERROR);
            auditMessageService.auditMessage(TokenMessagePair.Builder.create()
                    .withToken(dataRequest.getToken())
                    .withAuditMessage(authorisationErrorMessage));
        } else {
            // Create a consumer of the REST response's OutputStream, writing resource data to it
            stream = (OutputStream outputStream) -> {
                AuditableDataResponse auditableDataResponse = auditableDataService.read(auditableAuthorisedDataRequest, outputStream).join();
                auditMessageService.auditMessage(TokenMessagePair.Builder.create()
                        .withToken(dataRequest.getToken())
                        //send a message to the audit service of successfully processed request
                        .withAuditMessage(auditableDataResponse.getAuditSuccessMessage()));

                Optional.ofNullable(auditableDataResponse.getAuditErrorMessage())
                        .ifPresent((AuditErrorMessage errorMessage) -> {
                            //send a message to the audit service of an error occurred in processing a request
                            LOGGER.error("Error occurred processing the read : ", errorMessage.getError());
                            auditMessageService.auditMessage(TokenMessagePair.Builder.create()
                                    .withToken(dataRequest.getToken()).withAuditMessage(errorMessage));
                        });
            };
        }
        return new ResponseEntity<>(stream, httpStatus);
    }
}
