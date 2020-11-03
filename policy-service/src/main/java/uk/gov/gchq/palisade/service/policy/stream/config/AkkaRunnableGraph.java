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

package uk.gov.gchq.palisade.service.policy.stream.config;

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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;
import uk.gov.gchq.palisade.service.policy.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceAsyncProxy;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceHierarchyProxy;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration.Topic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.policy.PolicyApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;

    @Bean
    KafkaProducerService kafkaProducerService(
            final Sink<ProducerRecord<String, PolicyRequest>, CompletionStage<Done>> upstreamSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        return new KafkaProducerService(upstreamSink, upstreamConfig, materializer);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> runner(
            final Source<CommittableMessage<String, PolicyRequest>, Control> source,
            final Sink<Envelope<String, PolicyResponse, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final PolicyServiceAsyncProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        // Read messages from the stream source
        return source
                .map(message -> new Pair<>(message, Optional.ofNullable(message.record().value())))

                // Apply coarse-grained resource-level rules
                .mapAsync(PARALLELISM, messageAndRequest -> messageAndRequest.second()
                        // If is a real message, not start or end of stream messages then check the resource level rules
                        .map(policyRequest -> service.getResourceRules(policyRequest.getResource())
                                .thenApply(rules -> PolicyServiceHierarchyProxy.applyRulesToResource(policyRequest.getUser(), policyRequest.getResource(), policyRequest.getContext(), rules))
                                // If this is a proper request (not start or end of stream)
                                // If this is empty it could be a start/end of stream, or it could be a redacted resource, so keep track of both cases
                                .thenApply(resource -> new Pair<>(messageAndRequest.first(), Optional.of(new Pair<>(policyRequest, Optional.ofNullable(resource))))))
                        // If is a START/END of stream then treat as accessible
                        .orElse(CompletableFuture.completedFuture(new Pair<>(messageAndRequest.first(), Optional.empty()))))

                // Now we get the record level rules for all resources that weren't course grain filtered out
                .mapAsync(PARALLELISM, (Pair<CommittableMessage<String, PolicyRequest>, Optional<Pair<PolicyRequest, Optional<LeafResource>>>> messageRequestResource) -> messageRequestResource.second()
                        // If this is a proper request (not start or end of stream)
                        .map(requestAndResource -> requestAndResource.second()
                                // If there were resource rules and the resource was not redacted (course grain filtering)
                                .map(resource -> service.getRecordRules(resource)
                                        .thenApply(rules -> Optional.of(Optional.of(PolicyResponse.Builder.create(requestAndResource.first()).withResource(resource).withRules(rules)))))
                                // If the resource was redacted
                                .orElse(CompletableFuture.completedFuture(Optional.of(Optional.empty()))))
                        // If this is a start or end of stream
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                        // Keep track of original committable
                        .thenApply(response -> new Pair<>(messageRequestResource.first(), response)))

                // Now we build the producer message for kafka, adding the original committable
                // Filtered out resources use a pass through message, they aren't sent downstream but are committed upstream
                .map((Pair<CommittableMessage<String, PolicyRequest>, Optional<Optional<PolicyResponse>>> messageAndResponse) -> {
                    Committable committable = messageAndResponse.first().committableOffset();
                    ConsumerRecord<String, PolicyRequest> record = messageAndResponse.first().record();
                    return messageAndResponse.second()
                            // If this is a proper request
                            .map(maybeResponse -> maybeResponse
                                    // If the resource was not redacted
                                    .map(response -> ProducerMessage.single(new ProducerRecord<>(outputTopic.getName(), record.partition(), record.key(), response, record.headers()), committable))
                                    // If the resource was redacted
                                    .orElse(ProducerMessage.passThrough(committable)))
                            // If this is a start or end of stream
                            .orElse(ProducerMessage.single(new ProducerRecord<>(outputTopic.getName(), record.partition(), record.key(), null, record.headers()), committable));
                })

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }
}
