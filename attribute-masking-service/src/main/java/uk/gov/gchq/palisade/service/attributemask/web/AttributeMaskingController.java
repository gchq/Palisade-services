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

import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.request.AttributeMaskingResponse.Builder;
import uk.gov.gchq.palisade.service.attributemask.request.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.request.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.request.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.ErrorHandlingService;

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

    @PostMapping(value = "/storeRequestResult", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AttributeMaskingResponse> storeRequestResult(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody Optional<AttributeMaskingRequest> request) {
        boolean success;

        success = this.serviceStoreRequestResult(
                token,
                streamMarker,
                request
        );

        final HttpHeaders responseHeaders = new HttpHeaders();
        if (streamMarker != null) {
            responseHeaders.add(StreamMarker.HEADER, streamMarker.toString());
        }
        responseHeaders.add(Token.HEADER, token);
        final HttpStatus httpStatus;
        if (success) {
            httpStatus = HttpStatus.ACCEPTED;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(request.map(Builder::create).orElse(null), responseHeaders, httpStatus);
    }

    public boolean serviceStoreRequestResult(
            final @NonNull String token,
            final @Nullable StreamMarker streamMarker,
            final Optional<AttributeMaskingRequest> optionalRequest) {
        boolean success;

        // Only process if no stream marker
        if (streamMarker == null) {
            AttributeMaskingRequest request = optionalRequest.orElseThrow(() -> new IllegalArgumentException("If not " + StreamMarker.HEADER + " then body must not be null"));
            try {
                LOGGER.info("Storing request result for token {} and resourceId {}", token, request.getResource().getId());
                LOGGER.debug("Request is {}", request);
                this.attributeMaskingService.storeRequestResult(token,
                        request.getUser(),
                        request.getResource(),
                        request.getContext(),
                        request.getRules()
                );
                success = true;
            } catch (Exception ex) {
                success = false;
                AuditErrorMessage errorMessage = AuditErrorMessage.Builder.create(request, Collections.emptyMap())
                        .withError(ex);
                errorHandler.reportError(token, errorMessage);
            }
        } else {
            LOGGER.debug("Observed stream-marker {} for token {}", streamMarker, token);
            success = true;
        }
        return success;
    }
}
