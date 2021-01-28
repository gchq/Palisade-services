/*
 * Copyright 2018-2021 Crown Copyright
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
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.gchq.palisade.exception.ForbiddenException;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserPrepopulationFactory;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping(path = "/")
public class DataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataController.class);

    private final DataService dataService;
    private final AuditService auditService;
    private final StdSerialiserConfiguration serialiserConfig;

    /**
     * Constructor for a {@link DataController} instance.
     *
     * @param dataService             a {@link DataService} instance that will process the requests.
     * @param auditService            an {@link AuditService} instance that will audit successful data reads by the client.
     * @param serialiserConfiguration a {@link StdSerialiserConfiguration} that can be used to Pre-populate the {@link DataService}
     *                                with a {@link uk.gov.gchq.palisade.data.serialise.Serialiser}
     */
    public DataController(final DataService dataService,
                          final AuditService auditService,
                          final StdSerialiserConfiguration serialiserConfiguration) {
        this.dataService = dataService;
        this.auditService = auditService;
        this.serialiserConfig = serialiserConfiguration;
    }

    /**
     * REST endpoint to read a resource and return a streaming response.
     *
     * @param dataRequest the request to read the data from a leafResource
     * @return a stream of bytes representing the contents of the resource
     */
    @PostMapping(value = "/read/chunked", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> readChunked(@RequestBody final DataRequest dataRequest) {
        LOGGER.info("Invoking read (chunked): {}", dataRequest);
        DataReaderRequest readerRequest = dataService.authoriseRequest(dataRequest)
                .thenApply(maybeReadRequest -> maybeReadRequest
                        .orElseThrow(() -> new ForbiddenException(
                                String.format("The token '%s' is not authorised to access the leafResource '%s'", dataRequest.getToken(), dataRequest.getLeafResourceId()))))
                .join();

        StreamingResponseBody stream = (OutputStream outputStream) -> {
            Pair<AtomicLong, AtomicLong> recordsAudit = dataService.read(readerRequest, outputStream);
            AuditSuccessMessage successMessage = auditService.createSuccessMessage(dataRequest, readerRequest, recordsAudit.getFirst().get(), recordsAudit.getSecond().get());
            auditService.auditSuccess(dataRequest.getToken(), successMessage);
        };

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
                .forEach(entry -> dataService.addSerialiser(entry.getKey(), entry.getValue()));
    }

}
