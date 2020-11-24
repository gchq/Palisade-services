/*
 * Copyright 2020 Crown Copyright
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

import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;

import java.util.concurrent.CompletableFuture;

/**
 * Service for registering a data request. The service is expecting a unique token to identify this data request and
 * the data relevant to the request.  This data will be forwarded to a set of services, with each processing a step
 * towards the end goal of the data that satisfies the data request, and the necessary restrictions that are to be
 * imposed on the access to the data.
 */
public abstract class PalisadeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeService.class);

    private final Materializer materializer;
    private final CompletableFuture<Sink<TokenRequestPair, ?>> futureSink;

    /**
     * Instantiates a new Palisade service.
     *
     * @param materializer the materializer
     */
    public PalisadeService(final Materializer materializer) {
        this.futureSink = new CompletableFuture<>();
        this.materializer = materializer;
    }

    /**
     * Create token string.
     *
     * @param palisadeRequest the original request
     * @return the string
     */
    public abstract String createToken(PalisadeRequest palisadeRequest);

    /**
     * This method will forward the data to a Kafka data stream where it can be retrieved by the user-service.
     * The incoming request is in the form of a {@link PalisadeRequest} which contains all the information provided by the
     * client for registering this data request. Service will include a unique token to identify this data request and
     * the data relevant to the request.
     *
     * @param request information about the data, user requesting the data and the context of the request.
     * @return the token for later accessing the results of the request at the Filtered-Resource-Service.
     */
    public CompletableFuture<String> registerDataRequest(final PalisadeRequest request) {
        // needs to send the information to the stream
        // what is needed is to include the token, and the Palisade request as part of the source
        String token = this.createToken(request);
        TokenRequestPair incomingMessage = new TokenRequestPair(token, request);

        return futureSink.thenApply((Sink<TokenRequestPair, ?> sink) -> {
            Source.single(incomingMessage).runWith(sink, materializer);
            LOGGER.debug("registerDataRequest returning with token {} for request {}", token, request);
            return token;
        });
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
