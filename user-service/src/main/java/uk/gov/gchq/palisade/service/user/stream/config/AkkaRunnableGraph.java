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

package uk.gov.gchq.palisade.service.user.stream.config;

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

import uk.gov.gchq.palisade.service.user.model.AuditableUserResponse;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.user.service.UserServiceAsyncProxy;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.user.stream.SerDesConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.user.UserApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    @Bean
    KafkaProducerService kafkaProducerService(final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> sink,
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
            final Source<CommittableMessage<String, UserRequest>, Control> source,
            final Sink<Envelope<String, byte[], Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final UserServiceAsyncProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        // Get error topic from config
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        // Read messages from the stream source
        return source
                // Get the user from the userId, keeping track of original message and token
                .mapAsync(PARALLELISM, (CommittableMessage<String, UserRequest> message) -> {
                    Optional<UserRequest> userRequest = Optional.ofNullable(message.record().value());

                    /*
                     Return the user as a Pair of CommittableMessage<token,UserRequest>
                     and the auditableUserResponse from the UserServiceAsyncProxy.
                     If an exception was thrown, return the CommittableMessage with a null AuditableUserResponse
                    */
                    return userRequest.map(request -> service.getUser(request)
                            .thenApply(auditableUserResponse -> Pair.create(message, auditableUserResponse)))
                            .orElse(CompletableFuture.completedFuture(Pair.create(message, null)));
                })

                // Build producer record, copying the partition, keeping track of original message
                .map((Pair<CommittableMessage<String, UserRequest>, AuditableUserResponse> messageAndResponse) -> {
                    ConsumerRecord<String, UserRequest> requestRecord = messageAndResponse.first().record();
                    Optional<AuditableUserResponse> auditableUserResponse = Optional.ofNullable(messageAndResponse.second());
                    return auditableUserResponse.map(AuditableUserResponse::getAuditErrorMessage).map(audit ->
                            // Produce Audit Message
                            ProducerMessage.single(
                                    new ProducerRecord<>(errorTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                            SerDesConfig.errorValueSerialiser().serialize(null, audit), requestRecord.headers()),
                                    (Committable) messageAndResponse.first().committableOffset()))
                            .orElseGet(() ->
                                    // Produce Response
                                    ProducerMessage.single(
                                            new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                                    SerDesConfig.userValueSerialiser().serialize(null, auditableUserResponse.map(AuditableUserResponse::getUserResponse).orElse(null)), requestRecord.headers()),
                                            messageAndResponse.first().committableOffset()));
                })

                // Supervise, commit & produce to sink
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
                .toMat(sink, Consumer::createDrainingControl);
    }

}
