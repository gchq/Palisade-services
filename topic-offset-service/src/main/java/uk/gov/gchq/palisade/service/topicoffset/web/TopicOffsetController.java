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

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.ErrorHandlingService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.stream.ConsumerTopicConfiguration;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * REST Controller for Topic Offset Service.
 * Handles incoming requests to process message that are the start of a set of response messages for a specific request.
 * The commit offset associated with this start message will be used in an optimisation process later when it is needed
 * to start retrieving the data for the client.
 */
@RestController
@RequestMapping(path = "/stream-api")
public class TopicOffsetController {
    private final TopicOffsetService topicOffsetService;
    private final ErrorHandlingService errorHandler;
    private final Sink<ProducerRecord<String, TopicOffsetRequest>, CompletionStage<Done>> upstreamSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;

    public TopicOffsetController(final TopicOffsetService topicOffsetService,
                                 final ErrorHandlingService errorHandler,
                                 final Sink<ProducerRecord<String, TopicOffsetRequest>, CompletionStage<Done>> upstreamSink,
                                 final ConsumerTopicConfiguration upstreamConfig,
                                 final Materializer materializer) {
        this.topicOffsetService = topicOffsetService;
        this.errorHandler = errorHandler;
        this.upstreamSink = upstreamSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
    }

    /**
     * Takes the incoming RESTful request and evaluates if it is a start of a set of response messages for a specific
     * request.  If it is, a response message is forwarded that will contain the commit offset associated with this start
     * message.
     *
     * @param headers headers for the incoming request
     * @param request body {@link TopicOffsetRequest} of the request message
     * @return response {@link TopicOffsetResponse} with the commit offset for this client request
     */
    @PostMapping(value = "/topicOffset", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TopicOffsetResponse> serviceTopicOffset(
            final @RequestHeader Headers headers,
            final @RequestBody(required = false) TopicOffsetRequest request) {

        TopicOffsetResponse responseBody = null;
        HttpHeaders responseHeaders = null;
        HttpStatus httpStatus = HttpStatus.ACCEPTED;

        try {
            if (topicOffsetService.isOffsetForTopic(headers)) {
                responseHeaders = new HttpHeaders();
                responseHeaders.add(StreamMarker.HEADER, StreamMarker.START.toString());
                responseHeaders.add(Token.HEADER, headers.get(Token.HEADER));
            }

        } catch (Exception ex) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            errorHandler.reportError(headers.get(Token.HEADER), request, ex);

        }
        return new ResponseEntity<>(responseBody, responseHeaders, httpStatus);
    }

}
