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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.policy.model.AuditablePolicyRecordResponse;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceAsyncProxy;
import uk.gov.gchq.palisade.service.policy.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.policy.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.policy.stream.SerDesConfig;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.policy.PolicyApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    @Bean
    KafkaProducerService kafkaProducerService(
            final Sink<ProducerRecord<String, PolicyRequest>, CompletionStage<Done>> upstreamSink,
            final ConsumerTopicConfiguration upstreamConfig,
            final Materializer materializer) {
        return new KafkaProducerService(upstreamSink, upstreamConfig, materializer);
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
            final Source<CommittableMessage<String, PolicyRequest>, Control> source,
            final Sink<Envelope<String, byte[], Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final PolicyServiceAsyncProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        // Read messages from the stream source
        //  return source
        return source
                .map(committableMessage -> new Pair<>(committableMessage, committableMessage.record().value()))

                // Apply coarse-grained resource-level rules
                .mapAsync(PARALLELISM, messageAndRequest -> service.getResourceRules(messageAndRequest.second())
                        //need to change this only apply if we have rules and if we have a PolicyRequest
                        .thenApply(PolicyServiceAsyncProxy::applyRulesToResource)
                        .thenApply(modifiedAuditable -> Pair.create(messageAndRequest.first(), modifiedAuditable)))

                // Get the record level rules for all resources that weren't course grain filtered
                .mapAsync(PARALLELISM, messageAndModifiedRequest ->
                        service.getRecordRules(messageAndModifiedRequest.second())
                                //check to see if the first service request threw an exception and if so deal with it
                                .thenApply(modifiedResource -> modifiedResource.chain(messageAndModifiedRequest.second().getAuditErrorMessage()))
                                .thenApply(response -> Pair.create(messageAndModifiedRequest.first(), response))
                )

                // Build producer message, copying the partition, keeping track of original message
                .map((Pair<CommittableMessage<String, PolicyRequest>, AuditablePolicyRecordResponse> messageAndResponse) -> {
                    ConsumerRecord<String, PolicyRequest> requestRecord = messageAndResponse.first().record();
                    Committable committable = messageAndResponse.first().committableOffset();
                    return Optional.ofNullable(messageAndResponse.second().getAuditErrorMessage())
                            // Found an application error, produce an error message to be sent to the Audit service
                            .map(audit -> ProducerMessage.single(
                                    new ProducerRecord<>(errorTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                            SerDesConfig.errorValueSerializer().serialize(null, audit), requestRecord.headers()),
                                    committable))
                            //Found a response message, produce a policy message to be sent to the output
                            .orElse(ProducerMessage.single(
                                    new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                            SerDesConfig.ruleValueSerializer().serialize(null, messageAndResponse.second().getPolicyResponse()), requestRecord.headers()),
                                    committable));

                })
                // Send system errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);

    }
}
