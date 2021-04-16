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

package uk.gov.gchq.palisade.service.attributemask.stream.config;

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

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.common.Token;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.attributemask.stream.SerDesConfig;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link AttributeMaskingApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    @Bean
    KafkaProducerService kafkaProducerService(
            final Sink<ProducerRecord<String, AttributeMaskingRequest>, CompletionStage<Done>> upstreamSink,
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
            final Source<CommittableMessage<String, AttributeMaskingRequest>, Control> source,
            final Sink<Envelope<String, byte[], Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final AttributeMaskingService service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        // Read messages from the stream source
        return source
                // Extract token from message, keeping track of original message
                .map(committableMessage -> new Pair<>(committableMessage, new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset())))

                // Store authorised request in persistence, keeping track of original message and token
                .mapAsync(PARALLELISM, (Pair<CommittableMessage<String, AttributeMaskingRequest>, String> messageAndToken) ->
                        service.storeAuthorisedRequest(messageAndToken.second(), messageAndToken.first().record().value())
                                .thenApply(auditable -> Pair.create(messageAndToken.first(), auditable))
                )

                // Mask resource attributes, keeping track of original message
                .map(request -> Pair.create(request.first(), service.maskResourceAttributes(request.second().getAttributeMaskingRequest())
                        .chain(request.second().getAuditErrorMessage())))

                // Build producer message, copying the partition, keeping track of original message
                .map((Pair<CommittableMessage<String, AttributeMaskingRequest>, AuditableAttributeMaskingResponse> response) -> {
                    ConsumerRecord<String, AttributeMaskingRequest> requestRecord = response.first().record();
                    return Optional.ofNullable(response.second().getAuditErrorMessage()).map(audit ->
                            // Produce Audit Message
                            ProducerMessage.single(
                                    new ProducerRecord<>(errorTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                            SerDesConfig.errorValueSerializer().serialize(null, audit), requestRecord.headers()),
                                    (Committable) response.first().committableOffset()))
                            .orElseGet(() ->
                                    // Produce Masked Response
                                    ProducerMessage.single(
                                            new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(),
                                                    SerDesConfig.maskedResourceValueSerializer().serialize(null, response.second().getAttributeMaskingResponse()), requestRecord.headers()),
                                            (Committable) response.first().committableOffset()));
                })

                // Supervise, commit & produce to sink
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
                .toMat(sink, Consumer::createDrainingControl);
    }

}
