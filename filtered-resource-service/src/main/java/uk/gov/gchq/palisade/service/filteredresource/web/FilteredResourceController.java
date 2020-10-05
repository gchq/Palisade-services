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

import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorReporterService;
import uk.gov.gchq.palisade.service.filteredresource.service.FilteredResourceService;
import uk.gov.gchq.palisade.service.filteredresource.service.TopicOffsetService;

import java.util.Map;

/**
 * A REST interface mimicking the Kafka API to the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping("/api")
public class FilteredResourceController {
    private final ErrorReporterService errorReporterService;
    private final TopicOffsetService topicOffsetService;
    private final FilteredResourceService filteredResourceService;

    /**
     * Default constructor will be autowired by spring
     *
     * @param errorReporterService    the error-reporter-daemon to push REST requests to
     * @param topicOffsetService      the topic-token-offset-daemon to push REST requests to
     * @param filteredResourceService the filtered-resource-service to push REST requests to
     */
    public FilteredResourceController(final ErrorReporterService errorReporterService, final TopicOffsetService topicOffsetService, final FilteredResourceService filteredResourceService) {
        this.errorReporterService = errorReporterService;
        this.topicOffsetService = topicOffsetService;
        this.filteredResourceService = filteredResourceService;
    }

    /**
     * REST endpoint for the error reporter subsystem
     *
     * @param headers the REST headers containing the token for the request
     * @param error   the error thrown while processing a message
     * @return Whether or not the reporting of the error completed successfully
     */
    @PostMapping("/error")
    public ResponseEntity<Void> reportError(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody AuditErrorMessage error) {
        // TODO: Write this message to the upstream kafka queue
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * REST endpoint for the topic token offset subsystem
     *
     * @param headers       the REST headers containing the token for the request
     * @param offsetMessage the offset for the START_OF_STREAM message for this token
     * @return Whether or not the storing of the offset completed successfully
     */
    @PostMapping("/offset")
    public ResponseEntity<Void> storeTopicOffset(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody TopicOffsetMessage offsetMessage) {
        // TODO: Write this message to the upstream kafka queue
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * REST endpoint for pushing a filtered resource to the service, as if from kafka.
     * Messages with a stream marker will not be returned, but are used for control flow
     *
     * @param headers         the REST headers containing the token for the request
     * @param resourceRequest the request to the service containing a leafResource id
     * @return Whether or not the message was processed successfully
     */
    @PostMapping("/resource")
    public ResponseEntity<Void> acceptFilteredResource(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody(required = false) FilteredResourceRequest resourceRequest) {
        // TODO: Write this message to the upstream kafka queue
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
