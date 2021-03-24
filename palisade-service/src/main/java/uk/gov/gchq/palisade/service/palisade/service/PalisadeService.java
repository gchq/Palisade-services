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
package uk.gov.gchq.palisade.service.palisade.service;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.AuditablePalisadeSystemResponse;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for registering a data request. The service is expecting a unique token to identify this data request and
 * the data relevant to the request.  This data will be forwarded to a set of services, with each processing a step
 * towards the end goal of the data that satisfies the data request, and the necessary restrictions that are to be
 * imposed on the access to the data.
 */
public abstract class PalisadeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeService.class);

    private final Materializer materialiser;
    private final CompletableFuture<Sink<TokenRequestPair, ?>> futureSink;

    /**
     * Instantiates a new Palisade Service.
     *
     * @param materialiser the materialiser
     */
    PalisadeService(final Materializer materialiser) {
        this.futureSink = new CompletableFuture<>();
        this.materialiser = materialiser;
    }

    /**
     * Creates the unique token to attach to the request
     *
     * @param palisadeClientRequest the request from the client
     * @return the newly created token
     * @implNote Implement this class and override this method to add a different UUID generation method
     */
    public abstract String createToken(PalisadeClientRequest palisadeClientRequest);

    /**
     * This method will forward the data to the "request" Kafka topic where it can be retrieved by the User Service.
     * The incoming request is in the form of a {@link PalisadeClientRequest} which contains all the information provided by the
     * client for registering this data request. The service will include a unique token to identify this data request and
     * the data relevant to the request.
     * If an error is thrown, a {@link uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage} will be created and
     * forwarded to the "error" topic where it will be processed by the Audit Service
     *
     * @param request The request for data sent from the client, containing the resourceId that the attached user wants access to,
     *                and a reason for why they want access to the resource.
     * @return the unique token for later accessing the results of the request from the Filtered-Resource-Service.
     */
    public CompletableFuture<String> registerDataRequest(final PalisadeClientRequest request) {
        // Sends the information to the "request" topic
        String token = this.createToken(request);

        return futureSink.thenApply((Sink<TokenRequestPair, ?> sink) -> {
            AuditablePalisadeSystemResponse auditableRequest = AuditablePalisadeSystemResponse.Builder.create()
                    .withPalisadeRequest(request);
            TokenRequestPair requestPair = new TokenRequestPair(token, auditableRequest);
            Source.single(requestPair)
                    .runWith(sink, materialiser);
            LOGGER.debug("registerDataRequest returning with token {} for request {}", token, request);
            return token;
        });
    }

    /**
     * This method will forward the data to the "error" Kafka topic where it will be retrieved by the Audit Service.
     * The incoming request is in the form of a {@link PalisadeClientRequest} which contains all the information provided by the
     * client for registering this data request. The service will include a unique token to identify this data request and
     * the data relevant to the request and the error details.
     *
     * @param request    The request for data sent from the client, containing the resourceId that the attached user wants access to,
     *                   and a reason for why they want access to the resource.
     * @param token      the unique token associated with the request
     * @param attributes a {@link Map} of attributes associated with the request
     * @param error      the error encountered
     * @return a future completing once the error has been sent to the sink
     */
    public CompletableFuture<Void> errorMessage(final PalisadeClientRequest request, final String token,
                                                final Map<String, Object> attributes, final Throwable error) {
        // Sends the information to the "error" topic
        // We need to include the token, the PalisadeClientRequest information and the Error that occurred.
        AuditErrorMessage errorMessage = AuditErrorMessage.Builder.create(request, attributes).withError(error);
        AuditablePalisadeSystemResponse auditableRequest = AuditablePalisadeSystemResponse.Builder.create().withAuditErrorMessage(errorMessage);
        TokenRequestPair requestPair = new TokenRequestPair(token, auditableRequest);

        return futureSink.thenAccept(sink -> Source.single(requestPair).runWith(sink, materialiser));
    }

    /**
     * Register request sink.
     *
     * @param sink the sink
     */
    public void registerRequestSink(final Sink<TokenRequestPair, ?> sink) {
        this.futureSink.complete(sink);
    }
}
