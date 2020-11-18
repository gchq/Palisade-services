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

package uk.gov.gchq.palisade.service.filteredresource.stream.config;

import akka.Done;
import akka.japi.Pair;
import akka.japi.tuple.Tuple4;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ConsumerMessage.CommittableOffset;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Consumer.Control;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.repository.TokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.service.AuditService;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaComponentsConfig.PartitionedOffsetSourceFactory;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    public interface FilteredResourceSourceFactory {
        Source<Pair<FilteredResourceRequest, CommittableOffset>, Control> create(String token);
    }

    public interface AuditServiceSinkFactory {
        Sink<Pair<FilteredResourceRequest, CommittableOffset>, CompletionStage<Done>> create(String token);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    FilteredResourceSourceFactory filteredResourceSourceFactory(
            final PartitionedOffsetSourceFactory<String, FilteredResourceRequest> sourceFactory,
            final TokenOffsetPersistenceLayer persistenceLayer
    ) {
        return (String token) -> {
            // Set-up some objects to hold a minimal amount of state for this stream
            // These are mostly used for sanity checks
            // Has the stream START message been seen
            final AtomicBoolean observedStart = new AtomicBoolean(false);
            // Has a stream resource message been seen
            final AtomicBoolean observedResource = new AtomicBoolean(false);

            // Connect to the stream of resources using this token and the offset from the persistence layer
            final Source<CommittableMessage<String, FilteredResourceRequest>, Control> source = sourceFactory.create(token, persistenceLayer::findOffset);

            return source
                    // Extract token, (maybe) stream marker and (maybe) request from message headers
                    .map((CommittableMessage<String, FilteredResourceRequest> message) -> {
                        Headers headers = message.record().headers();
                        String messageToken = new String(headers.lastHeader(Token.HEADER).value(), Charset.defaultCharset());
                        // Get the StreamMarker (START or END) from headers (maybe Optional::empty if no StreamMarker)
                        Optional<StreamMarker> streamMarker = Optional.ofNullable(headers.lastHeader(StreamMarker.HEADER).value())
                                .map(String::new)
                                .map(StreamMarker::valueOf);
                        Optional<FilteredResourceRequest> request = Optional.ofNullable(message.record().value());
                        // Return each of the useful extracted objects, including the committable which will be passed to the audit service
                        return new Tuple4<>(messageToken, streamMarker, request, message.committableOffset());
                    })

                    // Filter for just this token's result set
                    .filter(tokenMarkerRequestCommittable -> token.equals(tokenMarkerRequestCommittable.t1()))

                    // Drop everything after the END message
                    .takeWhile(tokenMarkerRequestCommittable -> tokenMarkerRequestCommittable.t2()
                            .filter(StreamMarker.END::equals)
                            .isEmpty())

                    // We may have got an offset from persistence, in which case the stream has probably seeked to the START message.
                    // If not, the first element in the stream after filtering should still be the START message.
                    // This assumes that the START message, if present, is the first message (kafka has kept our messages ordered properly).
                    .map((Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableOffset> tokenMarkerRequestCommittable) -> {
                        if (!observedStart.get()) {
                            tokenMarkerRequestCommittable.t2()
                                    .filter(StreamMarker.START::equals)
                                    // Either a START message, or something else (resource or END marker)
                                    .ifPresentOrElse(
                                            startMarker -> observedStart.set(true),
                                            () -> LOGGER.error("Never observed START marker")
                                    );
                        }
                        return tokenMarkerRequestCommittable;
                    })

                    // We must check for the specific case where the client made a request and all results were redacted.
                    // This is indicative of the client querying something forbidden, so it is uniquely audited as an error.
                    .map((Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableOffset> tokenMarkerRequestCommittable) -> {
                        if (!observedResource.get()) {
                            // Either a masked resource or a stream marker (START or END)
                            tokenMarkerRequestCommittable.t3().ifPresentOrElse(
                                    resource -> observedResource.set(true),
                                    () -> tokenMarkerRequestCommittable.t2()
                                            .filter(StreamMarker.END::equals)
                                            // Either an END marker or a START marker
                                            // .ifPresentOrElse => or else START marker, which should be ignored
                                            .ifPresent(endMarker -> LOGGER.error("Never observed any resources"))
                            );
                        }
                        return tokenMarkerRequestCommittable;
                    })

                    // Strip start-of-stream and end-of-stream
                    // At runtime, this predicate (marker.isPresent/isEmpty) should be equivalent to request.isEmpty/Present
                    // For the sake of handling errors, it only matters that a message exists
                    .flatMapConcat(tokenMarkerRequestCommittable -> Source.fromJavaStream(() -> tokenMarkerRequestCommittable.t3()
                            .map(request -> new Pair<>(request, tokenMarkerRequestCommittable.t4()))
                            .stream()));
        };
    }


    /**
     * Take an incoming flow of {@link Optional} {@link FilteredResourceRequest}s and audit those which are present.
     * This should consume the provided {@link CommittableOffset} and commit it to kafka.
     * Empty elements should be passed through.
     *
     * @return an Akka {@link Flow} that will consume the committable offset, committing and auditing the requests
     */
    @Bean
    AuditServiceSinkFactory auditServiceSinkFactory(
            final Sink<Envelope<String, AuditSuccessMessage, Committable>, CompletionStage<Done>> auditSink,
            final ProducerTopicConfiguration topicConfiguration,
            final AuditService auditService
    ) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("success-topic");

        return (String token) -> {
            // Create kafka headers with client's token
            Headers headers = new RecordHeaders();
            headers.add(Token.HEADER, token.getBytes());
            // Map the token to a partition
            final int partition = Token.toPartition(token, outputTopic.getPartitions());

            // Create a processing flow before sinking to our sink
            return Flow.<Pair<FilteredResourceRequest, CommittableOffset>>create()
                    // Convert incoming FilteredResourceRequest to outgoing AuditSuccessMessage using the service instance
                    .map(resourceAndOffset -> new Pair<>(auditService.createSuccessMessage(resourceAndOffset.first()), resourceAndOffset.second()))

                    // Convert the audit message to a producer record, supplying the kafka topic, partition and headers
                    .map(requestAndOffset -> new Pair<>(
                            new ProducerRecord<>(
                                    outputTopic.getName(), partition,
                                    (String) null, requestAndOffset.first(), headers
                            ),
                            requestAndOffset.second()
                    ))

                    // Consume and apply the upstream committable to the kafka message
                    .map(recordAndOffset -> ProducerMessage.single(recordAndOffset.first(), (Committable) recordAndOffset.second()))

                    // Send to kafka to be consumed by the actual audit service
                    .toMat(auditSink, Keep.right());
        };
    }


    @Bean
    RunnableGraph<Control> tokenOffsetRunnableGraph() {
        // TODO: KafkaSource.to(ActorSink)
        return null;
    }

}
