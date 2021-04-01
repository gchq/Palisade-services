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

package uk.gov.gchq.palisade.service.policy.service;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import uk.gov.gchq.palisade.service.policy.common.Token;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Write the request and headers to the upstream topic.
 * These messages will then later be read by the service.
 * Intended for debugging only.
 */
public class KafkaProducerService {
    private final Sink<ProducerRecord<String, PolicyRequest>, CompletionStage<Done>> upstreamSink;
    private final ConsumerTopicConfiguration upstreamConfig;
    private final Materializer materializer;

    /**
     * Autowired constructor for the rest controller
     *
     * @param upstreamSink   a sink to the upstream topic
     * @param upstreamConfig the config for the topic (name, partitions, ...)
     * @param materializer   the akka system materializer
     */
    public KafkaProducerService(
            final Sink<ProducerRecord<String, PolicyRequest>, CompletionStage<Done>> upstreamSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        this.upstreamSink = upstreamSink;
        this.upstreamConfig = upstreamConfig;
        this.materializer = materializer;
    }

    /**
     * Takes a list of requests and processes each of them with the given headers.
     * These requests are each written to kafka using the supplied headers for all of them.
     *
     * @param headers  a map of request headers
     * @param requests a Collection of {@link PolicyRequest}
     * @return a future completing once all requests have been written to kafka
     */
    public CompletableFuture<Void> policyMulti(final Map<String, String> headers, final Collection<PolicyRequest> requests) {
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
        return Source
                // Akka reactive streams can't have null elements, so map to and from optional
                .fromJavaStream(() -> requests.stream().map(Optional::ofNullable))

                // Create kafka producer record from request
                .map(request -> new ProducerRecord<String, PolicyRequest>(topic.getName(), partition, null, request.orElse(null), kafkaHeaders))

                // Sink records to this service's upstream topic (not downstream)
                .toMat(this.upstreamSink, Keep.right())

                // Run the graph
                .run(this.materializer)

                // Return a CompletableFuture<Void> result
                .toCompletableFuture()
                .thenApply(x -> null);
    }
}
