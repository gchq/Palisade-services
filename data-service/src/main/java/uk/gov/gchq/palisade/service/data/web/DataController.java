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
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final DataService service;

    public DataController(final DataService service) {
        this.service = service;
    }

    @PostMapping(value = "/read", consumes = "application/json", produces = "application/json")
    public ReadResponse readSync(@RequestBody final ReadRequest request) {
        LOGGER.info("Invoking read: {}", request);
        ReadResponse response = read(request).join();
        LOGGER.info("Returning response: {}", response);
        return response;
    }

    // Taken from the following example: https://dzone.com/articles/streaming-data-with-spring-boot-restful-web-service
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final ReadRequest request) {
        LOGGER.info("Invoking readChunked: {}", request);
        StreamingResponseBody streamingResponseBody = outputStream -> {
            ReadResponse response = read(request).join();
            LOGGER.info("Writing response {} to output stream", response);
            response.writeTo(outputStream);
            outputStream.close();
            response.asInputStream().close();
            LOGGER.debug("IO streams completed and closed");
        };

        ResponseEntity<StreamingResponseBody> ret = new ResponseEntity<>(streamingResponseBody, HttpStatus.OK);
        LOGGER.info("Constructed and returning streamed response: {}", ret);
        return ret;
    }

    public CompletableFuture<ReadResponse> read(final ReadRequest request) {
        LOGGER.debug("Request will now be read: {}", request);
        return service.read(request);
    }
}
