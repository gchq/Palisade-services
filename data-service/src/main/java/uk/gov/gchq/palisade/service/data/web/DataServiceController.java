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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/")
public class DataServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataServiceController.class);

    private final DataService service;

    public DataServiceController(final DataService service) {
        this.service = service;
    }

    @PostMapping(value = "/read", consumes = "application/json", produces = "application/json")
    public ReadResponse readSync(@RequestBody final ReadRequest request) {
        LOGGER.debug("Invoking read: {}", request);
        return read(request).join();
    }

    // Taken from the following example: https://dzone.com/articles/streaming-data-with-spring-boot-restful-web-service
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final ReadRequest request) {

        StreamingResponseBody streamingResponseBody = outputStream -> {
            ReadResponse response = read(request).join();
            response.writeTo(outputStream);
            outputStream.close();
            response.asInputStream().close();
        };
        return new ResponseEntity(streamingResponseBody, HttpStatus.OK);
    }


    public CompletableFuture<ReadResponse> read(final ReadRequest request) {
        return service.read(request);
    }
}
