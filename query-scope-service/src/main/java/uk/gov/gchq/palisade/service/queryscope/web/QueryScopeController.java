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
package uk.gov.gchq.palisade.service.queryscope.web;

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

import uk.gov.gchq.palisade.service.queryscope.request.AuditErrorMessage;
import uk.gov.gchq.palisade.service.queryscope.request.QueryScopeRequest;
import uk.gov.gchq.palisade.service.queryscope.request.QueryScopeResponse;
import uk.gov.gchq.palisade.service.queryscope.request.StreamMarker;
import uk.gov.gchq.palisade.service.queryscope.request.Token;
import uk.gov.gchq.palisade.service.queryscope.service.AuditService;
import uk.gov.gchq.palisade.service.queryscope.service.QueryScopeService;

import java.util.Collections;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping(path = "/stream-api")
public class QueryScopeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryScopeController.class);

    private final QueryScopeService queryScopeService;
    private final AuditService auditService;

    public QueryScopeController(final QueryScopeService queryScopeService, final AuditService auditService) {
        this.queryScopeService = queryScopeService;
        this.auditService = auditService;
    }

    @PostMapping(value = "/storeRequestResult", consumes = "application/json", produces = "application/json")
    public ResponseEntity<QueryScopeResponse> storeRequestResult(
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestHeader(Token.HEADER) String token,
            final @RequestBody(required = false) QueryScopeRequest request) {
        boolean success;

        success = this.serviceStoreRequestResult(
                streamMarker,
                token,
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
        return new ResponseEntity<>(QueryScopeResponse.Builder.create(request), responseHeaders, httpStatus);
    }

    public boolean serviceStoreRequestResult(
            final @Nullable StreamMarker streamMarker,
            final @NonNull String token,
            final @Nullable QueryScopeRequest request) {
        boolean success;

        // Only process if no stream marker
        if (streamMarker == null) {
            requireNonNull(request);
            try {
                LOGGER.info("Storing request result for token {} and resourceId {}", token, request.getResource().getId());
                LOGGER.debug("Request is {}", request);
                this.queryScopeService.storeRequestResult(token,
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
                auditService.auditError(token, errorMessage);
            }
        } else {
            LOGGER.debug("Observed stream-marker {} for token {}", streamMarker, token);
            success = true;
        }
        return success;
    }
}
