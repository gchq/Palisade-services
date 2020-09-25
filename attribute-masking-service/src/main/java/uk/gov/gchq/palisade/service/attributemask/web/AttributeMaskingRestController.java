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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.model.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A REST interface mimicking the Kafka API to the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping(path = "/streamApi")
public class AttributeMaskingRestController {
    private final AttributeMaskingService service;

    /**
     * Autowired constructor for REST Controller, supplying the underlying service implementation
     *
     * @param service an implementation of the {@link AttributeMaskingService}
     */
    public AttributeMaskingRestController(final AttributeMaskingService service) {
        this.service = service;
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/maskAttributes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AttributeMaskingResponse> maskAttributes(
            final @RequestHeader MultiValueMap<String, String> headers,
            final @RequestBody(required = false) AttributeMaskingRequest request) {
        // Get token from headers
        String token = Optional.ofNullable(headers.get(Token.HEADER))
                .orElseThrow(() -> new NoSuchElementException("No token specified in headers"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Token specified in headers mapped to empty list"));

        // Process request
        AttributeMaskingResponse response = service.storeAuthorisedRequest(token, request)
                .thenApply(ignored -> service.maskResourceAttributes(request))
                .join();

        // Return result
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     * Takes a list of requests and processes each of them with the given headers
     *
     * @param headers  a multi-value map of http request headers
     * @param requests a list of requests
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/maskAttributes/multi", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<AttributeMaskingResponse>> maskAttributesMulti(
            final @RequestHeader MultiValueMap<String, String> headers,
            final @RequestBody List<AttributeMaskingRequest> requests) {
        // Process requests
        List<AttributeMaskingResponse> responses = requests.stream()
                .map(request -> maskAttributes(headers, request))
                .map(HttpEntity::getBody)
                .collect(Collectors.toList());

        // Return results
        return new ResponseEntity<>(responses, headers, HttpStatus.ACCEPTED);
    }

}
