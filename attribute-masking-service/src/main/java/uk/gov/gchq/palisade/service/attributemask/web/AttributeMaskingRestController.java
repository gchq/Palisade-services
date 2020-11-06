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
package uk.gov.gchq.palisade.service.attributemask.web;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.Token;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * A REST interface mimicking the Kafka API to the service.
 * POSTs to the controller write the request and headers to the upstream topic.
 * These messages will then later be read by the service.
 * Intended for debugging only.
 */
@RestController
@RequestMapping(path = "/api")
public class AttributeMaskingRestController {
    private final Sink<ProducerRecord<String, AttributeMaskingRequest>, CompletionStage<Done>> upstreamSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;

    /**
     * Autowired constructor for the rest controller
     *
     * @param upstreamSink   a sink to the upstream topic
     * @param upstreamConfig the config for the topic (name, partitions, ...)
     * @param materializer   the akka system materializer
     */
    public AttributeMaskingRestController(
            final Sink<ProducerRecord<String, AttributeMaskingRequest>, CompletionStage<Done>> upstreamSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        this.upstreamSink = upstreamSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     *
     * @param headers a multi-value map of http request headers
     * @param request the (optional) request itself
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/mask", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> maskAttributes(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody(required = false) AttributeMaskingRequest request) {
        // Process request as singleton list
        this.maskAttributesMulti(headers, Collections.singletonList(request));

        // Return result
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * REST endpoint for debugging the service, mimicking the Kafka API.
     * Takes a list of requests and processes each of them with the given headers
     *
     * @param headers  a multi-value map of http request headers
     * @param requests a list of requests
     * @return the response from the service, or an error if one occurred
     */
    @PostMapping(value = "/mask/multi", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> maskAttributesMulti(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody Collection<AttributeMaskingRequest> requests) {
        // Get token from headers
        String token = Optional.ofNullable(headers.get(Token.HEADER))
                .orElseThrow(() -> new NoSuchElementException("No token specified in headers"));

        // Get topic and calculate partition, unless this service has been assigned a partition
        Topic topic = this.upstreamConfig.getTopics().get("input-topic");
        int partition = Optional.ofNullable(topic.getAssignment())
                .orElseGet(() -> Token.toPartition(token, topic.getPartitions()));

        // Convert headers to kafka style
        List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset())))
                .collect(Collectors.toList());

        // Process requests
        // Akka reactive streams can't have null elements, so map to and from optional
        Source.fromJavaStream(() -> requests.stream().map(Optional::ofNullable))
                .map(request -> new ProducerRecord<String, AttributeMaskingRequest>(topic.getName(), partition, token, request.orElse(null), kafkaHeaders))
                .toMat(this.upstreamSink, Keep.right())
                .run(this.materializer)
                .toCompletableFuture()
                .join();

        // Return results
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
