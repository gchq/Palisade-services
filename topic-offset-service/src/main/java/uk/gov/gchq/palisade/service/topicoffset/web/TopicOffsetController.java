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

import uk.gov.gchq.palisade.service.topicoffset.message.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.message.Token;
import uk.gov.gchq.palisade.service.topicoffset.message.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.message.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;


/**
 * REST Controller for Topic Offset Service.
 * Handles incoming requests to process start of the stream messages, the first of the set of messages for a
 * specific client's data query.
 */
@RestController
@RequestMapping(path = "/stream-api")
public class TopicOffsetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicOffsetController.class);

    private final TopicOffsetService topicOffsetService;
    private final ErrorHandlingService errorHandler;


    public TopicOffsetController(final TopicOffsetService topicOffsetService, final ErrorHandlingService errorHandler) {
        this.topicOffsetService = topicOffsetService;
        this.errorHandler = errorHandler;
    }


    /**
     * Takes the incoming RESTful request and evaluates if it is a start of the stream message.  If it is, a response
     * is created that will be forwarded to indicate to allocate resources for the start of a new client request.
     * @param token unique identifier for all of the messages related to a client's request
     * @param streamMarker  indicator of this being either a start or end of stream
     * @param request the body of the request with the information from the request plus the related processed data.
     * @return message with the marker for the topic offset.
     */
    @PostMapping(value = "/topicOffset", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TopicOffsetResponse> serviceTopicOffset(
            final @RequestHeader(Token.HEADER) String token,
            final @RequestHeader(value = StreamMarker.HEADER, required = false) StreamMarker streamMarker,
            final @RequestBody(required = false) TopicOffsetRequest request) {

        TopicOffsetResponse responseBody = null;
        HttpHeaders responseHeaders = null;
        HttpStatus httpStatus = HttpStatus.ACCEPTED;


        try {
            responseBody = topicOffsetService.createTopicOffsetResponse(streamMarker);

        } catch (Exception ex) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            errorHandler.reportError(token, request, ex);
        }

        responseHeaders = new HttpHeaders();
        if (streamMarker != null) {
            responseHeaders.add(StreamMarker.HEADER, streamMarker.toString());
        }
        responseHeaders.add(Token.HEADER, token);

        return new ResponseEntity<>(responseBody, responseHeaders, httpStatus);

    }


}
