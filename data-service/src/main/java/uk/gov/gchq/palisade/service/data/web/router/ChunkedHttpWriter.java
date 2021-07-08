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

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.stream.javadsl.StreamConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.web.LeafResourceContentType;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Route for "/read/chunked
 */
public class ChunkedHttpWriter implements RouteSupplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedHttpWriter.class);
    private final AuditableDataService auditableDataService;
    private final AuditMessageService auditMessageService;

    public ChunkedHttpWriter(final AuditableDataService auditableDataService, final AuditMessageService auditMessageService) {
        this.auditableDataService = auditableDataService;
        this.auditMessageService = auditMessageService;
    }

    @Override
    public Route get() {
        // http://data-service/read/chunked -> stream of bytes
        return Directives.pathPrefix("read", () -> Directives.path("chunked", () -> Directives.withRangeSupport(() ->
                Directives.post(() -> Directives.entity(Jackson.unmarshaller(DataRequest.class), dataRequest -> {
                    LOGGER.info("Invoking read (chunked): {}", dataRequest);

                    // first with the client information about the request, retrieve the authorised resource information
                    AuditableAuthorisedDataRequest auditableAuthorisedDataRequest = auditableDataService.authoriseRequest(dataRequest).join();
                    AuditErrorMessage authorisationErrorMessage = auditableAuthorisedDataRequest.getAuditErrorMessage();

                    if (authorisationErrorMessage != null) {
                        LOGGER.error("Error occurred processing the authoriseRequest : ", authorisationErrorMessage.getError());
                        auditMessageService.auditMessage(TokenMessagePair.Builder.create()
                                .withToken(dataRequest.getToken())
                                .withAuditMessage(authorisationErrorMessage));
                        return Directives.complete(StatusCodes.FORBIDDEN);
                    } else {
                        // Create a consumer of the REST response's OutputStream, writing resource data to it
                        var leafResource = auditableAuthorisedDataRequest.getAuthorisedDataRequest().getResource();

                        // Maintain existing data-service API for now, create connected InputStream and OutputStream pair
                        var inputStream = new PipedInputStream();
                        var outputStream = new PipedOutputStream();
                        try {
                            outputStream.connect(inputStream);
                        } catch (IOException e) {
                            return Directives.complete(StatusCodes.INTERNAL_SERVER_ERROR);
                        }

                        AuditableDataResponse auditableDataResponse = auditableDataService.read(auditableAuthorisedDataRequest, outputStream)
                                .join();
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

                        var entity = HttpEntities.create(LeafResourceContentType.create(leafResource), StreamConverters.fromInputStream(() -> inputStream));
                        return Directives.complete(HttpResponse.create()
                                .withStatus(StatusCodes.OK)
                                .addHeaders(getDefaultHeadersForFoundResource(leafResource))
                                // Return the object data as a chunked stream instead of strict
                                .withEntity(entity));
                    }
                }))
        )));
    }

    private List<HttpHeader> getDefaultHeadersForFoundResource(final LeafResource leafResource) {
        var attributeHeaders = leafResource.getAttributes().entrySet().stream()
                .map(entry -> RawHeader.create(entry.getKey(), entry.getValue()));
        return attributeHeaders.collect(Collectors.toList());
    }
}
