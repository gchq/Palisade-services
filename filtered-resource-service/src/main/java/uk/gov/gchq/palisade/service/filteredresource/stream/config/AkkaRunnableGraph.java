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

package uk.gov.gchq.palisade.service.filteredresource.stream.config;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.japi.Pair;
import akka.japi.tuple.Tuple3;
import akka.japi.tuple.Tuple4;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ConsumerMessage.CommittableOffset;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Consumer.Control;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.filteredresource.common.Context;
import uk.gov.gchq.palisade.service.filteredresource.common.StreamMarker;
import uk.gov.gchq.palisade.service.filteredresource.common.Token;
import uk.gov.gchq.palisade.service.filteredresource.exception.NoResourcesObservedException;
import uk.gov.gchq.palisade.service.filteredresource.exception.NoStartMarkerObservedException;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditableWebSocketMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.exception.TokenErrorMessageController.TokenErrorMessageCommand;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController.TokenOffsetCommand;
import uk.gov.gchq.palisade.service.filteredresource.service.AuditEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.ErrorMessageEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;
import uk.gov.gchq.palisade.service.filteredresource.service.WebSocketEventService;
import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaComponentsConfig.PartitionedOffsetSourceFactory;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.gov.gchq.palisade.service.filteredresource.model.AuditMessage.SERVICE_NAME;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication}.
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);
    private static final Integer PARALLELISM = 1;
    private static final String UNKNOWN = "unknown";

    private static Sink<Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>>, NotUsed> observeResources(
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> auditErrorSink,
            final AtomicBoolean observedResource,
            final Topic errorTopic,
            final int partition
    ) {
        return Flow.<Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>>>create()
                .filter(tokenMarkerRequestMessage -> tokenMarkerRequestMessage.t3()
                        // If there are resources, set the observedResource boolean to true
                        .map((FilteredResourceRequest request) -> {
                            observedResource.set(true);
                            return false;
                        })
                        // If we haven't seen any resources, but receive the end of stream message, then audit it
                        .orElse(tokenMarkerRequestMessage.t2()
                                .filter(StreamMarker.END::equals)
                                .filter(endMarker -> !observedResource.get())
                                .isPresent()))

                .map((Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>> tokenMarkerRequestMessage) -> {
                    // Extract the headers
                    Headers headers = tokenMarkerRequestMessage.t4().record().headers();

                    // Build the error message
                    AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                            .withUserId(UNKNOWN)
                            .withResourceId(UNKNOWN)
                            .withContext(new Context().purpose(UNKNOWN))
                            .withAttributes(Collections.emptyMap())
                            .withError(new NoResourcesObservedException("No Resources were observed for token: " + tokenMarkerRequestMessage.t1()));

                    LOGGER.debug("NoResourcesObservedException thrown for token {}, on partition {} and topic {}", tokenMarkerRequestMessage.t1(), partition, errorTopic.getName());
                    // Create the ProducerRecord, on the error topic, on the right partition, with the audit error message

                    return new ProducerRecord<>(errorTopic.getName(), partition, (String) null, auditErrorMessage, headers);
                })
                // Audit the error
                .to(auditErrorSink);
    }

    @SuppressWarnings("java:S112")
    private static Sink<Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>>, NotUsed> observeStartOfStream(
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> auditErrorSink,
            final AtomicBoolean observedStart,
            final Topic errorTopic,
            final int partition
    ) {
        return Flow.<Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>>>create()
                .filter(tokenMarkerRequestMessage -> tokenMarkerRequestMessage.t2()
                        // Filter the results for a Start of Stream marker
                        .filter(StreamMarker.START::equals)
                        .map((StreamMarker startMarker) -> {
                            observedStart.set(true);
                            return false;
                        })
                        // If we haven't seen the start message then we want to audit it
                        .orElse(!observedStart.get()))

                .map((Tuple4<String, Optional<StreamMarker>, Optional<FilteredResourceRequest>, CommittableMessage<String, FilteredResourceRequest>> tokenMarkerRequestMessage) -> {
                    // Only audit once, not for each resource in which the Start Marker is missing
                    observedStart.set(true);

                    // Extract the headers
                    Headers headers = tokenMarkerRequestMessage.t4().record().headers();
                    // Extract the FilteredResourceRequest from the optional
                    return tokenMarkerRequestMessage.t3()
                            .map((FilteredResourceRequest filteredResourceRequest) -> {

                                // Build the error message
                                AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create().withUserId(filteredResourceRequest.getUserId())
                                        .withResourceId(filteredResourceRequest.getResourceId())
                                        .withContextNode(filteredResourceRequest.getContextNode())
                                        .withAttributes(Collections.emptyMap())
                                        .withError(new NoStartMarkerObservedException("No Start Marker was observed for token: " + tokenMarkerRequestMessage.t1()));

                                // Create the ProducerRecord, on the error topic, on the right partition, with the audit error message
                                LOGGER.debug("NoStartMarkerObservedException thrown for token {}, on partition {} and topic {}", tokenMarkerRequestMessage.t1(), partition, errorTopic.getName());
                                return new ProducerRecord<>(errorTopic.getName(), partition, (String) null, auditErrorMessage, headers);
                            });
                })

                // If we reach this error state with an end message, just ignore it
                .filter(Optional::isPresent)
                .map(Optional::get)

                // Limit the flow to run once
                // Audit the error
                .alsoTo(auditErrorSink)
                .to(Sink.foreach((ProducerRecord<String, AuditErrorMessage> tokenAndErrorMessage) -> {
                    throw new RuntimeException(tokenAndErrorMessage.value().getError());
                }));
    }

    @Bean
    WebSocketEventService websocketEventService(
            final ActorRef<TokenOffsetCommand> tokenOffsetController,
            final ActorRef<TokenErrorMessageCommand> tokenErrorController,
            final AuditServiceSinkFactory auditSinkFactory,
            final FilteredResourceSourceFactory resourceSourceFactory
    ) {
        return new WebSocketEventService(tokenOffsetController, tokenErrorController, auditSinkFactory, resourceSourceFactory);
    }

    @Bean
    KafkaProducerService kafkaProducerService(
            final Sink<ProducerRecord<String, FilteredResourceRequest>, CompletionStage<Done>> filteredResourceSink,
            final Sink<ProducerRecord<String, TopicOffsetMessage>, CompletionStage<Done>> topicOffsetSink,
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> auditErrorSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        return new KafkaProducerService(
                filteredResourceSink,
                topicOffsetSink,
                auditErrorSink,
                upstreamConfig,
                materializer);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return (Throwable ex) -> {
            LOGGER.error("Fatal error during stream processing, element will be dropped: ", ex);
            return Supervision.resumingDecider().apply(ex);
        };
    }

    @Bean
    FilteredResourceSourceFactory filteredResourceSourceFactory(final PartitionedOffsetSourceFactory<String, FilteredResourceRequest> sourceFactory,
                                                                final ProducerTopicConfiguration topicConfiguration,
                                                                final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> auditErrorSink) {
        return (String token, Long offset) -> {
            // Set-up some objects to hold a minimal amount of state for this stream
            // These are mostly used for sanity checks
            // Has the stream START message been seen
            final AtomicBoolean observedStart = new AtomicBoolean(false);
            // Has a stream resource message been seen
            final AtomicBoolean observedResource = new AtomicBoolean(false);
            // Get error topic from config
            Topic errorTopic = topicConfiguration.getTopics().get("error-topic");
            final int partition = Token.toPartition(token, errorTopic.getPartitions());

            // Connect to the stream of resources using this token and the offset from the persistence layer
            return sourceFactory.create(token, offset)
                    // Extract token, (maybe) stream marker and (maybe) request from message headers
                    .map((CommittableMessage<String, FilteredResourceRequest> message) -> {
                        Headers headers = message.record().headers();
                        String messageToken = new String(headers.lastHeader(Token.HEADER).value(), Charset.defaultCharset());
                        // Get the StreamMarker (START or END) from headers (maybe Optional::empty if no StreamMarker)
                        Optional<StreamMarker> streamMarker = Optional.ofNullable(headers.lastHeader(StreamMarker.HEADER))
                                .map(Header::value)
                                .map(String::new)
                                .map(StreamMarker::valueOf);
                        Optional<FilteredResourceRequest> request = Optional.ofNullable(message.record().value());
                        // Return each of the useful extracted objects, including the committable which will be passed to the audit service
                        return new Tuple4<>(messageToken, streamMarker, request, message);
                    })

                    // Filter for just this token's result set
                    .filter(tokenMarkerRequestMessage -> token.equals(tokenMarkerRequestMessage.t1()))

                    // Drop everything after the END message
                    .takeWhile(tokenMarkerRequestMessage -> tokenMarkerRequestMessage.t2()
                                    .filter(StreamMarker.END::equals)
                                    .isEmpty(),
                            true) // include END

                    // We may have got an offset from persistence, in which case the stream has probably seeked to the START message.
                    // If not, the first element in the stream after filtering should still be the START message.
                    // This assumes that the START message, if present, is the first message (kafka has kept our messages ordered properly).
                    .alsoTo(
                            observeStartOfStream(auditErrorSink, observedStart, errorTopic, partition))

                    // We must check for the specific case where the client made a request and all results were redacted.
                    // This is indicative of the client querying something forbidden, so it is uniquely audited as an error.
                    .alsoTo(
                            observeResources(auditErrorSink, observedResource, errorTopic, partition))

                    // Strip start-of-stream and end-of-stream
                    // At runtime, this predicate (marker.isPresent/isEmpty) should be equivalent to request.isEmpty/Present
                    // For the sake of handling errors, it only matters that a message exists
                    .flatMapConcat(tokenMarkerRequestMessage -> Source.fromJavaStream(() -> tokenMarkerRequestMessage.t3()
                            .map(request -> new Pair<>(request, (Committable) tokenMarkerRequestMessage.t4().committableOffset()))
                            .stream()));
        };
    }

    // Take an incoming flow of Optional FilteredResourceRequests and audit those which are present.
    // This should consume the provided {@link CommittableOffset} and commit it to kafka.
    // Empty elements should be passed through.
    @Bean
    AuditServiceSinkFactory auditServiceSinkFactory(
            final Sink<Envelope<String, AuditSuccessMessage, Committable>, CompletionStage<Done>> auditSink,
            final ProducerTopicConfiguration topicConfiguration,
            final AuditEventService auditEventService
    ) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("success-topic");

        return (String token) -> {
            // Create kafka headers with client's token
            Headers headers = new RecordHeaders();
            headers.add(Token.HEADER, token.getBytes(Charset.defaultCharset()));
            // Map the token to a partition
            final int partition = Token.toPartition(token, outputTopic.getPartitions());

            // Create a processing flow before sinking to our sink
            return Flow.<AuditableWebSocketMessage>create()

                    //Filter out anything that doesn't have a committable
                    .filter(message -> message.getCommittable() != null)

                    // Convert incoming FilteredResourceRequest to outgoing AuditSuccessMessage using the service instance
                    .map(message -> new Pair<>(auditEventService.createSuccessMessage(message.getFilteredResourceRequest()), message.getCommittable()))

                    // Convert the audit message to a producer record, supplying the kafka topic, partition and headers
                    .map(requestAndOffset -> new Pair<>(
                            new ProducerRecord<>(outputTopic.getName(), partition, (String) null, requestAndOffset.first(), headers),
                            requestAndOffset.second()))

                    // Consume and apply the upstream committable to the kafka message
                    .map(recordAndOffset -> ProducerMessage.single(recordAndOffset.first(), recordAndOffset.second()))

                    // Send to kafka to be consumed by the actual audit service
                    .toMat(auditSink, Keep.right());
        };
    }

    @Bean
    RunnableGraph<Control> tokenOffsetRunnableGraph(
            final Source<CommittableMessage<String, TopicOffsetMessage>, Control> tokenOffsetSource,
            final Sink<Committable, CompletionStage<Done>> committerSink,
            final OffsetEventService offsetEventService,
            final ActorRef<TokenOffsetCommand> tokenOffsetCtrl) {
        return tokenOffsetSource
                // Extract committable, token and message
                .map(committableMessage -> Tuple3.create(
                        committableMessage.committableOffset(),
                        new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset()),
                        committableMessage.record().value().commitOffset
                ))

                // Write offset to persistence
                .mapAsync(PARALLELISM, committableTokenOffset -> offsetEventService
                        .storeTokenOffset(committableTokenOffset.t2(), committableTokenOffset.t3())
                        .thenApply(ignored -> committableTokenOffset))

                // Alert actor system of new offset
                .alsoTo(Flow.<Tuple3<CommittableOffset, String, Long>>create()
                        .map(committableTokenOffset -> Pair.create(committableTokenOffset.t2(), committableTokenOffset.t3()))
                        .to(TokenOffsetController.asSetterSink(tokenOffsetCtrl)))

                // Commit processed message to kafka
                .to(Flow.<Tuple3<CommittableOffset, String, Long>>create()
                        .map(committableTokenOffset -> (Committable) committableTokenOffset.t1())
                        .to(committerSink));
    }

    @Bean
    RunnableGraph<Control> tokenErrorMessageRunnableGraph(final Source<CommittableMessage<String, AuditErrorMessage>, Control> tokenErrorMessageSource,
                                                          final Sink<Committable, CompletionStage<Done>> committerSink,
                                                          final ErrorMessageEventService errorMessageEventService) {
        return tokenErrorMessageSource
                // Extract committable, token and message
                .map(committableMessage -> Tuple3.create(
                        committableMessage.committableOffset(),
                        new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset()),
                        committableMessage.record().value()
                ))

                /*
                 Filter out anything from this service.
                 Used to prevent the feedback loop of an error being reported in the service being added to the error topic
                 and then added to this services persistence, only to get read here again.
                */
                .filter(auditErrorMessage -> !auditErrorMessage.t3().getServiceName().equals(SERVICE_NAME))

                // Write exception to persistence
                .mapAsync(PARALLELISM, committableTokenErrorMessage -> errorMessageEventService
                        .putAuditErrorMessage(committableTokenErrorMessage.t2(), committableTokenErrorMessage.t3())
                        .<Committable>thenApply(ignored -> committableTokenErrorMessage.t1()))

                // Commit processed message to kafka
                .to(committerSink);
    }

    /**
     * Factory for Akka {@link Source} of {@link FilteredResourceRequest}.
     * This should automatically connect to kafka at the given offset and filter headers by the given token.
     * The result is a stream of {@link FilteredResourceRequest}s to be returned to the client.
     */
    public interface FilteredResourceSourceFactory {
        /**
         * Factory for Akka {@link Source} of {@link FilteredResourceRequest}.
         * Connect to Kafka at the given offset for the partition decided by the token.
         * Filter messages by their token.
         *
         * @param token  the client's unique token for their request
         * @param offset the offset for this token retrieved by the {@link TokenOffsetController}
         * @return {@link Source} of {@link FilteredResourceRequest} for the client's request
         */
        Source<Pair<FilteredResourceRequest, Committable>, Control> create(String token, Long offset);
    }

    /**
     * Factory for Akka {@link Sink}s to the audit success queue for all {@link FilteredResourceRequest}s successfully
     * returned to the client. This auditing occurs just before the client is sent the resource, and should also
     * commit the given offset (for the upstream {@link FilteredResourceRequest} to kafka).
     */
    public interface AuditServiceSinkFactory {
        /**
         * Create a connection to the audit success queue for all {@link FilteredResourceRequest}s successfully
         * returned to the client. This auditing occurs just before the client is sent the resource, and should also
         * commit the given offset (for the upstream {@link FilteredResourceRequest} to kafka).
         *
         * @param token the token to re-attach as a header for kafka messages
         * @return {@link Sink} to the audit success queue for {@link FilteredResourceRequest}s and their {@link CommittableOffset}s
         */
        Sink<AuditableWebSocketMessage, CompletionStage<Done>> create(String token);
    }
}
