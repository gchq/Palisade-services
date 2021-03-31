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

package uk.gov.gchq.palisade.service.audit.service;

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

import uk.gov.gchq.palisade.service.audit.common.Token;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * A service mimicking the Kafka API to the service.
 * Write the request and headers to the upstream topic.
 * These messages will then later be read by the service.
 * Intended for debugging only.
 */
public class KafkaProducerService {
    private final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> upstreamErrorSink;
    private final Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> upstreamSuccessSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;

    /**
     * Autowired constructor for the rest controller
     *
     * @param upstreamErrorSink   a sink to the upstream error topic
     * @param upstreamSuccessSink a sink to the upstream success topic
     * @param upstreamConfig      the config for the topic (name, partitions, ...)
     * @param materializer        the akka system materializer
     */
    public KafkaProducerService(final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> upstreamErrorSink,
                                final Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> upstreamSuccessSink,
                                final ConsumerTopicConfiguration upstreamConfig,
                                final Materializer materializer) {
        this.upstreamErrorSink = upstreamErrorSink;
        this.upstreamSuccessSink = upstreamSuccessSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
    }

    /**
     * Takes a list of {@link AuditSuccessMessage}s and processes each of them with the given headers.
     * These requests are each written to the "success" kafka topic using the supplied headers for all of them.
     *
     * @param headers  a map of request headers
     * @param requests a list of requests
     * @return a {@link ResponseEntity} once all requests have been written to kafka
     */
    public ResponseEntity<Void> processSuccessRequest(final Map<String, String> headers,
                                                      final Collection<AuditSuccessMessage> requests) {
        // Get token from headers
        String token = Optional.ofNullable(headers.get(Token.HEADER))
                .orElseThrow(() -> new NoSuchElementException("No token specified in headers"));

        // Get topic and calculate partition, unless this service has been assigned a partition
        Topic topic = this.upstreamConfig.getTopics().get("success-topic");
        int partition = Optional.ofNullable(topic.getAssignment())
                .orElseGet(() -> Token.toPartition(token, topic.getPartitions()));

        // Convert headers to kafka style
        List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset())))
                .collect(Collectors.toList());

        // Process requests
        // Akka reactive streams can't have null elements, so map to and from optional
        Source.fromJavaStream(() -> requests.stream().map(Optional::ofNullable))
                .map(request -> new ProducerRecord<String, AuditSuccessMessage>(topic.getName(), partition, null, request.orElse(null), kafkaHeaders))
                .toMat(this.upstreamSuccessSink, Keep.right())
                .run(this.materializer)
                .toCompletableFuture()
                .join();

        // Return results
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * Takes a list of {@link AuditErrorMessage}s and processes each of them with the given headers.
     * These requests are each written to the "error" kafka topic using the supplied headers for all of them.
     *
     * @param headers  a map of request headers
     * @param requests a list of requests
     * @return a {@link ResponseEntity} once all requests have been written to kafka
     */
    @SuppressWarnings("unused")
    public ResponseEntity<Void> processErrorRequest(final Map<String, String> headers,
                                                    final Collection<AuditErrorMessage> requests) {
        // Get token from headers
        String token = Optional.ofNullable(headers.get(Token.HEADER))
                .orElseThrow(() -> new NoSuchElementException("No token specified in headers"));

        // Get topic and calculate partition, unless this service has been assigned a partition
        Topic topic = this.upstreamConfig.getTopics().get("error-topic");
        int partition = Optional.ofNullable(topic.getAssignment())
                .orElseGet(() -> Token.toPartition(token, topic.getPartitions()));

        // Convert headers to kafka style
        List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(Charset.defaultCharset())))
                .collect(Collectors.toList());

        // Process requests
        // Akka reactive streams can't have null elements, so map to and from optional
        Source.fromJavaStream(() -> requests.stream().map(Optional::ofNullable))
                .map(request -> new ProducerRecord<String, AuditErrorMessage>(topic.getName(), partition, null, request.orElse(null), kafkaHeaders))
                .toMat(this.upstreamErrorSink, Keep.right())
                .run(this.materializer)
                .toCompletableFuture()
                .join();

        // Return results
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
