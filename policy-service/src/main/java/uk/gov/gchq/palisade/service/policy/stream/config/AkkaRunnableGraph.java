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
import akka.japi.tuple.Tuple3;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Consumer.Control;
import akka.kafka.javadsl.Consumer.DrainingControl;
import akka.stream.ActorAttributes;
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
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.PolicyResponse;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceAsyncProxy;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.request.Policy;

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
                        .map(policyRequest -> service
                                .canAccess(policyRequest.getUser(), policyRequest.getContext(), policyRequest.getResource())
                                .thenApply(accessible -> accessible.map(leafResource -> new Tuple3<>(messageAndRequest.first(), messageAndRequest.second(), leafResource)))
                        )
                        // If is a START/END of stream then treat as accessible
                        .orElse(CompletableFuture.completedFuture(Optional.of(new Tuple3<>(messageAndRequest.first(), messageAndRequest.second(), null)))))

                // Filter out resources that are completely redacted
                .flatMapConcat(optional -> Source.fromJavaStream(optional::stream))

                // Having filtered out any resources the user doesn't have access to in the function above, we now build the map
                // of resource to record level rule policies. If there are resource level rules for a record then there SHOULD
                // be record level rules. Either list may be empty, but they should at least be present
                .mapAsync(PARALLELISM, (Tuple3<CommittableMessage<String, PolicyRequest>, Optional<PolicyRequest>, LeafResource> messageRequestResource) -> messageRequestResource.t2()
                        .map((PolicyRequest request) ->
                                service.getPolicy(request.getResource())
                                        .thenApply(policy -> policy.get().getRecordRules())
                                        .thenApply(rules -> PolicyResponse.Builder.create(request).withResource(messageRequestResource.t3()).withRules(rules)))
                        .orElse(CompletableFuture.completedFuture(null))
                        .thenApply(r -> new Pair<>(messageRequestResource.t1(), r)))

                // Build producer record, copying the partition, keeping track of original message
                .map((Pair<CommittableMessage<String, PolicyRequest>, PolicyResponse> messageTokenResponse) -> {
                    ConsumerRecord<String, PolicyRequest> requestRecord = messageTokenResponse.first().record();
                    return new Pair<>(messageTokenResponse.first(), new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(),
                            messageTokenResponse.second(), requestRecord.headers()));
                })

                // Build producer message, applying the committable pass-thru consuming the original message
                .map(messageAndRecord -> ProducerMessage.single(messageAndRecord.second(), (Committable) messageAndRecord.first().committableOffset()))

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }
}
