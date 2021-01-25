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
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;

import java.io.OutputStream;

/**
 * Controller for data-service.  Provides the front end RESTFul web service for resources that have already been
 * registered with the Palisade Service.  The request is in the form of information that will uniquely identify the
 * resource request and will return a data stream of the response data.
 *
 */
@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final AuditableDataService auditableDataService;
    private final AuditMessageService auditMessageService;

    /**
     *
     * @param auditableDataService service for providing auditable data for the request
     * @param auditMessageService service for sending audit messages
     */
    public DataController(final AuditableDataService auditableDataService, final AuditMessageService auditMessageService) {
        this.auditableDataService = auditableDataService;
        this.auditMessageService = auditMessageService;
    }

    /**
     * REST endpoint to read a resource and return a streaming response.
     *
     * @param dataRequestModel the request to read the data from a leafResource
     * @return a stream of bytes representing the contents of the resource
     */
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final DataRequestModel dataRequestModel) {
        LOGGER.debug("Invoking read (chunked): {}", dataRequestModel);

        HttpStatus httpStatus = HttpStatus.ACCEPTED;
        StreamingResponseBody stream = null;

        //first with the client information about the request, retrieve the authorised resource information
        AuditableDataReaderRequest auditableDataReaderRequest = auditableDataService.authoriseRequest(dataRequestModel).join();
        AuditErrorMessage firstErrorMessage = auditableDataReaderRequest.getAuditErrorMessage();

        if (firstErrorMessage != null) {
            LOGGER.error("Error occurred processing the authoriseRequest for  {}", firstErrorMessage);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            auditMessageService.auditMessage(new TokenMessagePair(dataRequestModel.getToken(), firstErrorMessage)).join();
        } else {

            //retrieve the outputstream that links to the resources
            stream = (OutputStream outputStream) -> {
                AuditableDataReaderResponse auditableDataReaderResponse = auditableDataService.read(auditableDataReaderRequest, outputStream).join();
                auditMessageService.auditMessage(new TokenMessagePair(dataRequestModel.getToken(), auditableDataReaderResponse.getAuditSuccessMessage())).join();

                AuditErrorMessage secondErrorMessage = auditableDataReaderResponse.getAuditErrorMessage();
                if (secondErrorMessage != null) {
                    LOGGER.error("Error occurred processing the authoriseRequest for  {}", secondErrorMessage);
                    auditMessageService.auditMessage(new TokenMessagePair(dataRequestModel.getToken(), secondErrorMessage)).join();
                }
            };
        }
        return new ResponseEntity<>(stream, httpStatus);
    }
}
