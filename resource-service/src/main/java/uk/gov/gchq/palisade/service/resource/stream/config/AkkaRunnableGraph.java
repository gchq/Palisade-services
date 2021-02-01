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

package uk.gov.gchq.palisade.service.resource.stream.config;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Consumer.Control;
import akka.kafka.javadsl.Consumer.DrainingControl;
import akka.stream.ActorAttributes;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.resource.stream.SerDesConfig;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.resource.ResourceApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    private interface ProducerMessageBuilder {
        /**
         * Build a {@link ProducerMessage} output from {@link CommittableMessage} input and {@link AuditableResourceResponse} service output.
         * If the service produced a response, prefer to build a message from the {@link AuditableResourceResponse#getAuditErrorMessage()} first.
         * If the service did not produce a response and instead returned {@link Optional#empty()} then produce a {@link akka.kafka.ProducerMessage.PassThroughMessage}
         * to commit the input.
         *
         * @param committableMessage the upstream input
         * @param auditableResourceResponse the service's output (or empty if the upstream input carried no request)
         * @return a downstream message using upstream's committable offset
         */
        ProducerMessage.Envelope<String, byte[], Committable> build(CommittableMessage<String, ResourceRequest> committableMessage, Optional<AuditableResourceResponse> auditableResourceResponse);
    }

    @Bean
    KafkaProducerService kafkaProducerService(final Sink<ProducerRecord<String, ResourceRequest>, CompletionStage<Done>> sink,
                                              final ConsumerTopicConfiguration upstreamConfig,
                                              final Materializer materializer) {
        return new KafkaProducerService(sink, upstreamConfig, materializer);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return (Throwable ex) -> {
            LOGGER.error("Fatal error during stream processing, element will be dropped: ", ex);
            return Supervision.resumingDecider().apply(ex);
        };
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> runner(
            final Source<CommittableMessage<String, ResourceRequest>, Control> source,
            final Sink<Envelope<String, byte[], Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final ResourceServicePersistenceProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        // Get error topic from config
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        // Builder for producer messages capturing all bits of context required
        ProducerMessageBuilder messageBuilder = (message, optionalResponse) -> optionalResponse
                .map(response -> Optional.ofNullable(response.getAuditErrorMessage())
                        // If there was an error thrown in the service
                        .map(audit -> ProducerMessage.single(
                                new ProducerRecord<>(errorTopic.getName(), message.record().partition(), message.record().key(),
                                        SerDesConfig.errorValueSerializer().serialize(null, audit), message.record().headers()),
                                (Committable) message.committableOffset()))
                        // If a resource was returned successfully
                        .orElseGet(() -> ProducerMessage.single(
                                new ProducerRecord<>(outputTopic.getName(), message.record().partition(), message.record().key(),
                                        SerDesConfig.resourceValueSerializer().serialize(null, response.getResourceResponse()),
                                        message.record().headers()),
                                message.committableOffset())))
                // If this was the Optional.empty() marking the end of the stream
                .orElseGet(() -> ProducerMessage.passThrough(message.committableOffset()));

        // Read messages from the stream source
        return source
                .map(message -> new Pair<>(message, Optional.ofNullable(message.record().value())))

                // Get the request optional from the Pair
                .flatMapConcat(messageAndRequest -> messageAndRequest.second()
                        // If the request optional is not empty then we need to process the message
                        // Get a stream of resources from the implemented resource-service
                        .map(request -> service.getResourcesById(request)
                                // Make the stream of resources an Optional
                                .map(Optional::of)
                                // Add empty optional to the end of the stream so that we can identify
                                // when we have processed the last resource in the stream.
                                // This will allow us to then acknowledge the original upstream message
                                .concat(Source.single(Optional.empty()))
                                // Build the producer message
                                .map(auditableResourceResponse -> messageBuilder.build(messageAndRequest.first(), auditableResourceResponse))
                                // Ignore the materialization value
                                .mapMaterializedValue(ignored -> NotUsed.notUsed())
                        )
                        // If the request optional is empty then this is a StreamMarker (Start or End) message
                        // Pass the StreamMarker message downstream without processing
                        .orElseGet(() -> {
                            Optional<AuditableResourceResponse> emptyResponse = Optional.of(AuditableResourceResponse.Builder.create().withResponseAndError(null, null));
                            return Source.single(messageBuilder.build(messageAndRequest.first(), emptyResponse));
                        }))

                // Supervise if an exception is thrown we haven't caught
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }

}
