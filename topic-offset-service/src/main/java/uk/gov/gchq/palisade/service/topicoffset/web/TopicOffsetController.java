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
package uk.gov.gchq.palisade.service.topicoffset.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.topicoffset.request.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.request.Token;
import uk.gov.gchq.palisade.service.topicoffset.request.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.request.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;

import java.util.Optional;


@RestController
@RequestMapping(path = "/stream-api")
public class TopicOffsetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicOffsetController.class);

    private final TopicOffsetService topicOffsetService;
    private final ErrorHandlingService errorHandlingService;

    public TopicOffsetController(final TopicOffsetService topicOffsetService, final ErrorHandlingService errorHandlingService) {
        this.topicOffsetService = topicOffsetService;
        this.errorHandlingService = errorHandlingService;
    }

    @PostMapping(value = "/topicOffset", consumes = "application/json", produces = "application/json")
    public Optional<ResponseEntity<TopicOffsetResponse>> serviceTopicOffset(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody Optional<TopicOffsetRequest> request) {

        //*****************temp code
        Optional<TopicOffsetResponse> responseBody = null;
        HttpHeaders responseHeaders = null;
        HttpStatus httpStatus = HttpStatus.ACCEPTED;


        try {
            responseBody = topicOffsetService.createTopicOffsetResponse(streamMarker);

        } catch (Throwable ex) {
            //later
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            //errorHandlingService
        }

        responseHeaders = new HttpHeaders();
        if (streamMarker != null) {
            responseHeaders.add(StreamMarker.HEADER, streamMarker.toString());
        }
        responseHeaders.add(Token.HEADER, token);

        //fix later- ResponserEntity is not optional, responseBody is
        return Optional.of(new ResponseEntity(responseBody, responseHeaders, httpStatus));
        //*****************temp code

    }


}
