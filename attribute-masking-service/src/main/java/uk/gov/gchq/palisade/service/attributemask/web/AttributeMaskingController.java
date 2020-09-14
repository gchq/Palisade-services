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
package uk.gov.gchq.palisade.service.attributemask.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse.Builder;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A REST interface mimicking the Kafka API to the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping(path = "/streamApi")
public class AttributeMaskingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMaskingController.class);

    private final AttributeMaskingService attributeMaskingService;
    private final ErrorHandlingService errorHandler;

    public AttributeMaskingController(final AttributeMaskingService attributeMaskingService, final ErrorHandlingService errorHandler) {
        this.attributeMaskingService = attributeMaskingService;
        this.errorHandler = errorHandler;
    }

    /**
     * REST endpoint for debugging and playing with the service, mimicking the Kafka API.
     * At least one of the streamMarker or request must be present. If both are present, the headers take precedent
     * over the body (treated as streamMarker rather than request).
     *
     * @param token           the token for the client's request, stored in headers
     * @param streamMarker    the (optional) start/end of stream marker for the client's request, stored in headers
     * @param request the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/maskAttributes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AttributeMaskingResponse> restMaskAttributes(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody(required = false) AttributeMaskingRequest request) {
        // Assume success
        HttpStatus httpStatus = HttpStatus.ACCEPTED;
        // Lots of control flow choices depending upon whether this was a streamMarker or request
        Optional<LeafResource> optionalMaskedResource = Optional.empty();

        try {
            // Try to store with service
            optionalMaskedResource = this.serviceMaskAttributes(
                    token,
                    streamMarker,
                    request
            );
        } catch (IOException ex) {
            // Audit error appropriately (REST-fully)
            // If we failed with a StreamMarker message, there's no real way to audit it, so throw NoSuchElementException
            errorHandler.reportError(token, requireNonNull(request), ex);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Prepare body
        final LeafResource maskedResource = optionalMaskedResource
                .orElse(null);
        final AttributeMaskingResponse responseBody = Optional.ofNullable(request)
                .map(Builder::create)
                .map(x -> x.withResource(maskedResource))
                .orElse(null);

        // Prepare headers
        final HttpHeaders responseHeaders = new HttpHeaders();
        if (streamMarker != null) {
            responseHeaders.add(StreamMarker.HEADER, streamMarker.toString());
        }
        responseHeaders.add(Token.HEADER, token);

        // Return result
        return new ResponseEntity<>(responseBody, responseHeaders, httpStatus);
    }

    /**
     * Common service interface between Kafka and the REST API.
     * At least one of the streamMarker or the request must be non-null, or throws a NullPointerException.
     * If both are non-null, precedence is given to the stream-marker
     *
     * @param token                the token for the client's request, stored in headers
     * @param nullableStreamMarker the (optional) start/end of stream marker for the client's request, stored in headers
     * @param nullableRequest      the (optional) request itself
     * @return the response from the service
     * @throws IOException if an error occurred (as well as RuntimeExceptions)
     */
    Optional<LeafResource> serviceMaskAttributes(
            final String token,
            final @Nullable StreamMarker nullableStreamMarker,
            final @Nullable AttributeMaskingRequest nullableRequest) throws IOException {
        // Only process if no stream marker
        if (nullableStreamMarker == null) {
            final AttributeMaskingRequest request = requireNonNull(nullableRequest, "One of streamMarker and request must be non-null");
            return Optional.of(processRequest(token, request));

        } else {
            processStreamMarker(token, nullableStreamMarker);
            return Optional.empty();
        }
    }

    private LeafResource processRequest(final String token, final AttributeMaskingRequest request) throws IOException {
        // Store authorised request with service
        LOGGER.info("Storing request result for token {} and resourceId {}", token, request.getResource().getId());
        LOGGER.debug("Request is {}", request);
        // Throw an exception if the request was not stored successfully
        attributeMaskingService.storeAuthorisedRequest(token,
                request.getUser(),
                request.getResource(),
                request.getContext(),
                request.getRules()
        );
        // Return the masked resource
        return attributeMaskingService.maskResourceAttributes(request.getResource());
    }

    private void processStreamMarker(final String token, final StreamMarker streamMarker) {
        // Nothing to do if this is a stream marker, transparently pass on the message
        LOGGER.debug("Observed stream-marker {} for token {}", streamMarker, token);
    }

}
