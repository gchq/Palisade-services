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

import akka.Done;
import akka.NotUsed;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity.Chunked;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.authorisation.AuditableAuthorisationService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;
import uk.gov.gchq.palisade.service.data.web.LeafResourceContentType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Route for "/read/chunked"
 */
public class ReadChunkedDataService extends AbstractDataService {

    /**
     * Construct a new instance of a {@link ReadChunkedDataService}, delegating construction to the {@link AbstractDataService} superclass.
     *
     * @param readers      collection of {@link DataReader}s which may or may not {@link DataReader#accepts(LeafResource)} a requested {@link LeafResource},
     *                     where the first found reader that accepts the resource will be used to {@link DataReader#read(LeafResource)} it
     * @param serialisers  map from serialiser names (decided here using {@link LeafResource#getSerialisedFormat()}) and serialiser classes to use for
     *                     constructing {@link Serialiser}s to (de)serialise bytes into records (so rules can be applied)
     * @param dataService  instance of {@link AuditableAuthorisationService} to decide whether access to a given resource should be granted, and with which rules
     *                     to apply to this data read
     * @param auditService sink to send {@link uk.gov.gchq.palisade.service.data.model.AuditMessage}s to on success or failure of a data read
     */
    public ReadChunkedDataService(
            final Collection<DataReader> readers,
            final Map<String, Class<Serialiser<?>>> serialisers,
            final AuditableAuthorisationService dataService,
            final AuditMessageService auditService) {
        super(readers, serialisers, dataService, auditService);
    }

    @Override
    public Route get() {
        // /read/chunked
        return Directives.pathPrefix("read", () -> Directives.pathPrefix("chunked", () -> Directives.pathEndOrSingleSlash(() ->
                // POST with header Range: <Range> and body DataRequest
                Directives.post(() -> Directives.withRangeSupport(() -> Directives.entity(Jackson.unmarshaller(DataRequest.class), request ->
                        Directives.completeWithFuture(authorisationService.authoriseRequest(request)
                                .thenApply((AuditableAuthorisedDataRequest authorisation) -> {
                                    // Decide HTTP Content-Type header and Status-Code
                                    ContentType contentType;
                                    StatusCode statusCode;
                                    if (authorisation.getAuthorisedDataRequest() != null) {
                                        // Access was granted for this resource and token
                                        var leafResource = authorisation.getAuthorisedDataRequest().getResource();
                                        contentType = LeafResourceContentType.create(leafResource);
                                        statusCode = StatusCodes.OK;
                                    } else {
                                        // Access was denied for this resource and token
                                        contentType = ContentTypes.NO_CONTENT_TYPE;
                                        statusCode = StatusCodes.FORBIDDEN;
                                    }

                                    // Use AbstractResponseWriter super method
                                    Source<ByteString, CompletionStage<Done>> responseSource = super.defaultSourceWithAuditing(authorisation);
                                    // Create streamed (chunked) HTTP response entity
                                    Chunked responseEntity = HttpEntities.create(contentType, responseSource);
                                    // Return HTTP response
                                    return HttpResponse.create()
                                            .withStatus(statusCode)
                                            .withEntity(responseEntity);
                                })))
                )))));
    }

    @Override
    protected Flow<ByteString, ByteString, NotUsed> transformResponse() {
        // Data returned to the client is exactly what was returned after deserialisation/redaction/serialisation
        return Flow.create();
    }
}
