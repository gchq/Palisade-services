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

package uk.gov.gchq.palisade.service.filteredresource.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorReporterDaemon;
import uk.gov.gchq.palisade.service.filteredresource.service.FilteredResourceService;
import uk.gov.gchq.palisade.service.filteredresource.service.TokenOffsetDaemon;

/**
 * A REST interface mimicking the Kafka API to the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping("/streamApi")
public class FilteredResourceController {
    private final ErrorReporterDaemon errorReporterDaemon;
    private final TokenOffsetDaemon tokenOffsetDaemon;
    private final FilteredResourceService filteredResourceService;

    /**
     * Default constructor will be autowired by spring
     *
     * @param errorReporterDaemon     the error-reporter-daemon to push REST requests to
     * @param tokenOffsetDaemon       the topic-token-offset-daemon to push REST requests to
     * @param filteredResourceService the filtered-resource-service to push REST requests to
     */
    public FilteredResourceController(final ErrorReporterDaemon errorReporterDaemon, final TokenOffsetDaemon tokenOffsetDaemon, final FilteredResourceService filteredResourceService) {
        this.errorReporterDaemon = errorReporterDaemon;
        this.tokenOffsetDaemon = tokenOffsetDaemon;
        this.filteredResourceService = filteredResourceService;
    }

    /**
     * REST endpoint for the error reporter subsystem
     *
     * @param token the token for which an error was thrown
     * @param error the error thrown while processing a message
     * @return Whether or not the reporting of the error completed successfully
     */
    @PostMapping("/reportError")
    public ResponseEntity<Void> reportError(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestBody Throwable error) {
        errorReporterDaemon.reportError(token, error);

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * REST endpoint for the topic token offset subsystem
     *
     * @param token         a token for which there are filtered resources available on a kafka queue
     * @param offsetMessage the offset for the START_OF_STREAM message for this token
     * @return Whether or not the storing of the offset completed successfully
     */
    @PostMapping("/topicOffset")
    public ResponseEntity<Void> storeTopicOffset(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestBody TopicOffsetMessage offsetMessage) {
        tokenOffsetDaemon.storeTokenOffset(token, offsetMessage.queuePointer);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * REST endpoint for pushing a filtered resource to the service, as if from kafka.
     * Messages with a stream marker will not be returned, but are used for control flow
     *
     * @param token           the token for the request
     * @param streamMarker    the (optional) stream marker for this message
     * @param resourceRequest the request to the service containing a leafResource id
     * @return Whether or not the message was processed successfully
     */
    @PostMapping("/acceptFilteredResource")
    public ResponseEntity<Void> acceptFilteredResource(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody(required = false) FilteredResourceRequest resourceRequest) {
        filteredResourceService.spawnProcessorForToken(token);

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
