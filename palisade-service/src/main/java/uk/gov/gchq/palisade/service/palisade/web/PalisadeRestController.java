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
package uk.gov.gchq.palisade.service.palisade.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeResponse;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the palisade-service.  Provides the front end RESTFul web service for Palisade Service.  Incoming
 * requests will be forwarded to a set of services with each processing an aspect of the request.  The response will
 * be a reference that can be used to view the data when it has been processed.
 */
@RestController
@RequestMapping(path = "/api")
public class PalisadeRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeRestController.class);

    private final PalisadeService palisadeService;

    /**
     * Constructor for the palisade-service Controller.
     *
     * @param palisadeService      service which processes the request
     */
    public PalisadeRestController(final PalisadeService palisadeService) {
        this.palisadeService = palisadeService;
    }

    /**
     * Takes the incoming RESTful request, forwards the request for further processing and returns a URL that can
     * later be used at the filtered-resource-service to view the requested data.
     *
     * @param request body {@link PalisadeRequest} of the request message
     * @return dataURL a unique URL to identify the data available for this data request
     */
    @PostMapping(value = "/registerDataRequest", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PalisadeResponse> registerDataRequest(
            final @RequestBody PalisadeRequest request) {
        LOGGER.debug("registerDataRequest called with request {}", request);

        HttpStatus httpStatus = HttpStatus.ACCEPTED;
        PalisadeResponse palisadeResponse = null;
        String token = "";

        try {
            //instead of join we could do a .get(Time) and specify a timeout
            token = palisadeService.registerDataRequest(request).join();
            palisadeResponse = new PalisadeResponse(token);
            LOGGER.debug("registerDataRequest token is {}", token);
        } catch (Exception e) {
            LOGGER.error("PalisadeRestController Exception thrown", e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            Map<String, Object> attributes = new HashMap<>();
            palisadeService.errorMessage(request, token, attributes, e);
        }
        return ResponseEntity.status(httpStatus)
                .body(palisadeResponse);
    }
}
