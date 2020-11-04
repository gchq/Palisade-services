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
package uk.gov.gchq.palisade.service.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.KafkaProducerService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A REST interface mimicking the Kafka API to the service.
 * POSTs to the controller write the request and headers to the upstream topic.
 * These messages will then later be read by the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping(path = "/api")
public class UserRestController {

    private final KafkaProducerService service;

    /**
     * Autowired constructor for the rest controller
     *
     * @param service the service used to process a {@link UserRequest}
     */
    public UserRestController(final KafkaProducerService service) {
        this.service = service;
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/user", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> userRequest(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody(required = false) UserRequest request) {

        // Process the request and return results
        return service.processRequest(headers, Collections.singletonList(request));
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     * Takes a list of requests and processes each of them with the given headers
     *
     * @param headers  a multi-value map of http request headers
     * @param requests a list of requests
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/user/multi", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> userRequestMulti(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody Collection<UserRequest> requests) {

        // Process the request and return results
        return service.processRequest(headers, requests);
    }
}
