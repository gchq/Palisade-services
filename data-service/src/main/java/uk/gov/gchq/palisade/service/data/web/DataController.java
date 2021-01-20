/*
 * Copyright 2019 Crown Copyright
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
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.KafkaDataService;

import java.io.OutputStream;

@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final KafkaDataService kafkaDataService;
    private final AuditService auditService;

    private static final String EMPTY_TOKEN = "";


    /**
     *
     * @param kafkaDataService sdf
     * @param auditService asd
     */
    public DataController(final KafkaDataService kafkaDataService, final AuditService auditService) {
        this.kafkaDataService = kafkaDataService;
        this.auditService = auditService;
    }

    /**
     * REST endpoint to read a resource and return a streaming response.
     *
     * @param dataRequestModel the request to read the data from a leafResource
     * @return a stream of bytes representing the contents of the resource
     */
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final DataRequestModel dataRequestModel) {
        LOGGER.info("Invoking read (chunked): {}", dataRequestModel);

        HttpStatus httpStatus = HttpStatus.ACCEPTED;
        StreamingResponseBody stream = null;

        //first given the client information about the request, retrieve the authorised resource information
        AuditableDataReaderRequest auditableDataReaderRequest = kafkaDataService.authoriseRequest(dataRequestModel).join();
        AuditErrorMessage auditErrorMessage = auditableDataReaderRequest.getAuditErrorMessage();

        if (auditErrorMessage != null) {
            LOGGER.error("Error occurred processing the authoriseRequest for  {}", auditErrorMessage);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            auditService.auditMessage(AuditableDataReaderResponse.Builder.create()
                    .withToken(EMPTY_TOKEN)
                    .withSuccessMessage(null)
                    .withAuditErrorMessage(auditErrorMessage));
        } else {

            //retrieve the outputstream that links to the resources
            stream = (OutputStream outputStream) -> {
                AuditableDataReaderResponse auditableDataReaderResponse = kafkaDataService.read(auditableDataReaderRequest, outputStream).join();
                auditService.auditMessage(auditableDataReaderResponse);
            };
        }
        return new ResponseEntity<>(stream, httpStatus);
    }
}
