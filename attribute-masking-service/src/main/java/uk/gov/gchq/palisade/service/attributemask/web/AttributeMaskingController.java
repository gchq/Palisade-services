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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse.Builder;
import uk.gov.gchq.palisade.service.attributemask.request.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.request.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.request.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping(path = "/stream-api")
public class AttributeMaskingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMaskingController.class);

    private final AttributeMaskingService attributeMaskingService;
    private final ErrorHandlingService errorHandler;

    public AttributeMaskingController(final AttributeMaskingService attributeMaskingService, final ErrorHandlingService errorHandler) {
        this.attributeMaskingService = attributeMaskingService;
        this.errorHandler = errorHandler;
    }

    @PostMapping(value = "/maskAttributes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AttributeMaskingResponse> restMaskAttributes(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody Optional<AttributeMaskingRequest> request) {
        // Assume success
        HttpStatus httpStatus = HttpStatus.ACCEPTED;
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
            final AuditErrorMessage errorMessage = AuditErrorMessage.Builder.create(request.orElseThrow(), Collections.emptyMap())
                    .withError(ex);
            errorHandler.reportError(token, errorMessage);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Prepare body
        final LeafResource maskedResource = optionalMaskedResource
                .orElse(null);
        final AttributeMaskingResponse responseBody = request
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

    public Optional<LeafResource> serviceMaskAttributes(
            final @NonNull String token,
            final @Nullable StreamMarker streamMarker,
            final Optional<AttributeMaskingRequest> optionalRequest) throws IOException {
        // Only process if no stream marker
        if (streamMarker == null) {
            final AttributeMaskingRequest request = optionalRequest.orElseThrow(() -> new IllegalArgumentException("One of " + StreamMarker.HEADER + " and body must be non-null"));
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
            return Optional.of(attributeMaskingService.maskResourceAttributes(request.getResource()));

        } else {
            // Nothing to do if this is a stream marker, transparently pass on the message
            LOGGER.debug("Observed stream-marker {} for token {}", streamMarker, token);
            return Optional.empty();
        }
    }

}
