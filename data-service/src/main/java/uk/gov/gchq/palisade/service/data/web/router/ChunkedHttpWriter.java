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

package uk.gov.gchq.palisade.service.data.web.router;

import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.model.WebSocketMessage;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.service.WebSocketEventService;

import java.io.OutputStream;
import java.util.Optional;

/**
 * Route for "/read/chunked
 */
public class HttpStreamWriter implements RouteSupplier {
    private final AuditableDataService auditableDataService;
    private final AuditMessageService auditMessageService;
    private final ObjectMapper mapper;


    @Override
    public Route get() {
        // http://data-service/read/chunked -> stream of bytes
        return Directives.pathPrefix("resource", () -> Directives.path((String token) -> {
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
        }));
    }
}
