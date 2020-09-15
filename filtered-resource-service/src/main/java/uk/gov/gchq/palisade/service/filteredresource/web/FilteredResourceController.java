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

import uk.gov.gchq.palisade.service.filteredresource.message.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.message.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.message.Token;
import uk.gov.gchq.palisade.service.filteredresource.message.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorReporterDaemon;
import uk.gov.gchq.palisade.service.filteredresource.service.FilteredResourceService;
import uk.gov.gchq.palisade.service.filteredresource.service.TokenOffsetDaemon;

@RestController
@RequestMapping("/streamApi")
public class FilteredResourceController {
    private final ErrorReporterDaemon errorReporterDaemon;
    private final TokenOffsetDaemon tokenOffsetDaemon;
    private final FilteredResourceService service;

    public FilteredResourceController(final ErrorReporterDaemon errorReporterDaemon, final TokenOffsetDaemon tokenOffsetDaemon, final FilteredResourceService service) {
        this.errorReporterDaemon = errorReporterDaemon;
        this.tokenOffsetDaemon = tokenOffsetDaemon;
        this.service = service;
    }

    @PostMapping("/reportError")
    public ResponseEntity<Void> reportError(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestBody Throwable error) {
        errorReporterDaemon.reportError(token, error);

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @PostMapping("/topicOffset")
    public ResponseEntity<Void> storeTopicOffset(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestBody TopicOffsetMessage offsetMessage) {
        tokenOffsetDaemon.storeTokenOffset(token, offsetMessage.queuePointer);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping("/acceptFilteredResource")
    public ResponseEntity<Void> acceptFilteredResource(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody(required = false) FilteredResourceRequest resourceRequest) {
        service.spawnProcessorForToken(token);

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
