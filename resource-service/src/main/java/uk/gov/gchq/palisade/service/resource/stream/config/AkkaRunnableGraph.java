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

package uk.gov.gchq.palisade.service.resource.stream.config;

import akka.Done;
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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse.Builder;
import uk.gov.gchq.palisade.service.resource.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.service.resource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration.Topic;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.resource.ResourceApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {

    @Bean
    KafkaProducerService kafkaProducerService(final Sink<ProducerRecord<String, ResourceRequest>, CompletionStage<Done>> sink,
                                              final ConsumerTopicConfiguration upstreamConfig,
                                              final Materializer materializer) {
        return new KafkaProducerService(sink, upstreamConfig, materializer);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> runner(
            final Source<CommittableMessage<String, ResourceRequest>, Control> source,
            final Sink<Envelope<String, ResourceResponse, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final StreamingResourceServiceProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        // Read messages from the stream source
        return source
                .map(message -> new Pair<>(message, Optional.ofNullable(message.record().value())))
                // Get a stream of resources from an iterator
                .flatMapConcat(messageAndRequest -> messageAndRequest.second()
                        // If the request optional is not empty then we need to process the message
                        // Get a stream of resources from the implemented resource-service
                        .map(request -> Source.fromIterator(() -> service.getResourcesById(request.resourceId))
                                // Make the stream of resources an Optional
                                .map(Optional::of)
                                // Add empty optional to the end of the stream so that we can identify
                                // when we have processed the last resource in the stream.
                                // This will allow us to then acknowledge the original upstream message
                                .concat(Source.single(Optional.empty()))
                                // Build the producer record for each leaf resource within the Optional
                                .map(resourceOptional -> resourceOptional
                                        .map(leafResource -> new ProducerRecord<>(
                                                outputTopic.getName(),
                                                messageAndRequest.first().record().partition(),
                                                messageAndRequest.first().record().key(),
                                                Builder.create(messageAndRequest.first().record().value()).withResource(leafResource),
                                                messageAndRequest.first().record().headers())
                                        )
                                )
                                // Build the producer message for each record in the optional
                                .map(recordOptional -> recordOptional
                                        // If the optional has a leaf resource object we send the message to the output topic
                                        .map(record -> ProducerMessage.single(record, (Committable) null))
                                        // When we get to the final empty optional in the stream we need to acknowledge the original message
                                        .orElse(ProducerMessage.passThrough(messageAndRequest.first().committableOffset()))
                                )
                        )
                        // If the request optional is empty this is either a `START` or `END` message and is passed to the downstream topic with no value
                        .orElseGet(() ->
                                Source.single(ProducerMessage.single(new ProducerRecord<>(
                                        outputTopic.getName(),
                                        messageAndRequest.first().record().partition(),
                                        messageAndRequest.first().record().key(),
                                        null,
                                        messageAndRequest.first().record().headers()), messageAndRequest.first().committableOffset())
                                )
                        )
                )

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }

}
