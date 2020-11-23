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

package uk.gov.gchq.palisade.service.audit.stream.config;

import akka.Done;
import akka.japi.Pair;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Committer;
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

import uk.gov.gchq.palisade.service.audit.model.AuditMessage;
import uk.gov.gchq.palisade.service.audit.model.Token;
import uk.gov.gchq.palisade.service.audit.service.AuditService;
import uk.gov.gchq.palisade.service.audit.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.audit.AuditApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;

    @Bean
    KafkaProducerService kafkaProducerService(final Sink<ProducerRecord<String, AuditMessage>, CompletionStage<Done>> sink,
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
            final Source<CommittableMessage<String, AuditMessage>, Control> source,
            final Sink<Envelope<String, Boolean, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final CommitterSettings committerSettings,
            final AuditService service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        // Read messages from the stream source
        return source
                // Extract token from message, keeping track of original message
                .map(committableMessage -> new Pair<>(
                        committableMessage,
                        new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset())
                ))

                // Audit the message (this can be either a success or error message)
                .mapAsync(PARALLELISM, (Pair<CommittableMessage<String, AuditMessage>, String> messageAndToken) -> {
                    CommittableMessage<String, AuditMessage> message = messageAndToken.first();
                    Optional<AuditMessage> auditMessage = Optional.ofNullable(message.record().value());
                    return auditMessage.map(request -> service.audit(request, messageAndToken.second())
                            .thenApply(result -> new Pair<>(message, result)))
                            .orElse(CompletableFuture.completedFuture(new Pair<>(message, null)));
                })

                // Build producer record, copying the partition, keeping track of original message
                .map((Pair<CommittableMessage<String, AuditMessage>, Boolean> messageTokenResponse) -> {
                    ConsumerRecord<String, AuditMessage> requestRecord = messageTokenResponse.first().record();
                    return new Pair<>(messageTokenResponse.first(), new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(),
                            messageTokenResponse.second(), requestRecord.headers()));
                })

                // Build producer message, applying the committable pass-thru consuming the original message
                .map(messageAndRecord -> ProducerMessage.single(
                        messageAndRecord.second(), (Committable) messageAndRecord.first().committableOffset())
                )

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(Committer.sink(committerSettings), Consumer::createDrainingControl);
    }

}
