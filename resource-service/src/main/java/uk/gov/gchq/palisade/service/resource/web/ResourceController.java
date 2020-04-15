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

package uk.gov.gchq.palisade.service.resource.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;

@RestController
@RequestMapping(path = "/")
public class ResourceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

    private final StreamingResourceServiceProxy service;

    public ResourceController(final StreamingResourceServiceProxy service) {
        this.service = service;
    }

    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> getResourcesById(@RequestBody final GetResourcesByIdRequest request) {
        LOGGER.info("Invoking getResourceById: {}", request);
        StreamingResponseBody stream = out -> service.getResourcesById(request.getResourceId(), out);
        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> getResourcesByResource(@RequestBody final GetResourcesByResourceRequest request) {
        LOGGER.info("Invoking getResourcesByResource: {}", request);
        StreamingResponseBody stream = out -> service.getResourcesByResource(request.getResource(), out);
        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> getResourcesByType(@RequestBody final GetResourcesByTypeRequest request) {
        LOGGER.info("Invoking getResourceByType: {}", request);
        StreamingResponseBody stream = out -> service.getResourcesByType(request.getType(), out);
        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> getResourcesBySerialisedFormat(@RequestBody final GetResourcesBySerialisedFormatRequest request) {
        LOGGER.info("Invoking getResourcesBySerialisedFormatRequest: {}", request);
        StreamingResponseBody stream = out -> service.getResourcesBySerialisedFormat(request.getSerialisedFormat(), out);
        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    public Boolean addResource(@RequestBody final AddResourceRequest request) {
        LOGGER.info("Invoking addResource: {}", request);
        Boolean response = service.addResource(request.getResource());
        LOGGER.info("Returning response: {}", response);
        return response;
    }
}
