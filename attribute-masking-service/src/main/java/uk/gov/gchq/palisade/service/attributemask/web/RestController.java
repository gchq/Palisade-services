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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.io.IOException;
import java.util.Optional;

/**
 * A REST interface mimicking the Kafka API to the service.
 * Intended for debugging only.
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping(path = "/streamApi")
public class RestController extends MarkedStreamController {

    public RestController(final AttributeMaskingService attributeMaskingService) {
        super(attributeMaskingService);
    }

    /**
     * REST endpoint for debugging and playing with the service, mimicking the Kafka API.
     * At least one of the streamMarker or request must be present. If both are present, the headers take precedent
     * over the body (treated as streamMarker rather than request).
     *
     * @param token        the token for the client's request, stored in headers
     * @param streamMarker the (optional) start/end of stream marker for the client's request, stored in headers
     * @param request      the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/maskAttributes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AttributeMaskingResponse> maskAttributes(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody(required = false) AttributeMaskingRequest request) {
        // Assume success
        HttpStatus httpStatus = HttpStatus.ACCEPTED;

        // Lots of control flow choices depending upon whether this was a streamMarker or request
        Optional<AttributeMaskingResponse> response = Optional.empty();
        try {
            // Try to store with service
            response = this.processRequestOrStreamMarker(
                    token,
                    streamMarker,
                    request
            );
        } catch (IOException ex) {
            // Audit error appropriately (REST-fully)
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Prepare headers
        final HttpHeaders responseHeaders = new HttpHeaders();
        if (streamMarker != null) {
            responseHeaders.add(StreamMarker.HEADER, streamMarker.toString());
        }
        responseHeaders.add(Token.HEADER, token);

        // Return result
        return new ResponseEntity<>(response.orElse(null), responseHeaders, httpStatus);
    }

}
