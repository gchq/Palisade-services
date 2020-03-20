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
package uk.gov.gchq.palisade.service.data.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.data.request.AddSerialiserRequest;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.service.DataService;

@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final DataService service;

    public DataController(final DataService service) {
        this.service = service;
    }

    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final ReadRequest request) {
        LOGGER.info("Invoking read (chunked): {}", request);
        StreamingResponseBody stream = out -> service.read(request).accept(out);

        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    @PostMapping(value = "/addSerialiser", consumes = "application/json", produces = "application/json")
    public Boolean addSerialiser(@RequestBody final AddSerialiserRequest request) {
        LOGGER.info("Invoking addSerialiser: {}", request);
        Boolean response = service.addSerialiser(request);

        LOGGER.info("Request processed. Result: {}", response);
        return response;
    }


}
