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

package uk.gov.gchq.palisade.service.data.service.writer;

import akka.NotUsed;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.AuditMessageService;
import uk.gov.gchq.palisade.service.data.service.AuditableDataService;
import uk.gov.gchq.palisade.service.data.service.reader.DataReader;
import uk.gov.gchq.palisade.service.data.web.LeafResourceContentType;

import java.util.Collection;
import java.util.Map;

/**
 * Route for "/read/chunked"
 */
public class ChunkedHttpWriter extends AbstractResponseWriter {
    public ChunkedHttpWriter(
            final Collection<DataReader> readers,
            final Map<String, Class<Serialiser<?>>> serialisers,
            final AuditableDataService dataService,
            final AuditMessageService auditService) {
        super(readers, serialisers, dataService, auditService);
    }

    @Override
    public Route get() {
        return Directives.path("read", () -> Directives.path("chunked", () -> Directives.withRangeSupport(() ->
                Directives.entity(Jackson.unmarshaller(DataRequest.class), request ->
                        Directives.completeWithFuture(dataService.authoriseRequest(request).thenApply(auditableAuthorisedDataRequest -> {
                            // Decide HTTP Content-Type header and Status-Code
                            ContentType contentType;
                            StatusCode statusCode;
                            if (auditableAuthorisedDataRequest.getAuthorisedDataRequest() != null) {
                                var leafResource = auditableAuthorisedDataRequest.getAuthorisedDataRequest().getResource();
                                contentType = LeafResourceContentType.create(leafResource);
                                statusCode = StatusCodes.OK;
                            } else {
                                contentType = ContentTypes.NO_CONTENT_TYPE;
                                statusCode = StatusCodes.FORBIDDEN;
                            }

                            var responseSource = this.defaultRunnableGraph(auditableAuthorisedDataRequest);
                            var responseEntity = HttpEntities.create(contentType, responseSource);
                            // Return HTTP response
                            return HttpResponse.create()
                                    .withStatus(statusCode)
                                    .withEntity(responseEntity);
                        }))
                ))));
    }

    @Override
    protected Flow<ByteString, ByteString, NotUsed> transformResponse() {
        // Data returned to the client is exactly what was returned after deserialisation/redaction/serialisation
        return Flow.create();
    }
}
