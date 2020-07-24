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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.service.DataService;

@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final DataService service;
    private final StdSerialiserConfiguration serialiserConfig;

    /**
     * Constructor for a {@link DataController} instance.
     *
     * @param service                   a {@link DataService} instance that will process the requests.
     * @param serialiserConfiguration   a {@link StdSerialiserConfiguration} that can be used to Pre-populate the {@link DataService}
     *                                  with a {@link uk.gov.gchq.palisade.data.serialise.Serialiser}
     */
    public DataController(final DataService service,
                          final StdSerialiserConfiguration serialiserConfiguration) {
        this.service = service;
        this.serialiserConfig = serialiserConfiguration;
    }

    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final ReadRequest request) {
        LOGGER.info("Invoking read (chunked): {}", request);
        StreamingResponseBody stream = out -> service.read(request, out);

        LOGGER.info("Streaming response: {}", stream);
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    /**
     * This method will add a {@link uk.gov.gchq.palisade.data.serialise.Serialiser} to the
     * {@link DataService} using the details provided in a yaml file.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initPostConstruct() {
        // Add serialiser to the data-service
        LOGGER.info("Prepopulating using serialiser config: {}", serialiserConfig.getClass());
        serialiserConfig.getSerialisers().stream()
                .map(StdSerialiserPrepopulationFactory::build)
                .peek(entry -> LOGGER.debug(entry.toString()))
                .forEach(entry -> service.addSerialiser(entry.getKey(), entry.getValue()));
    }

}
