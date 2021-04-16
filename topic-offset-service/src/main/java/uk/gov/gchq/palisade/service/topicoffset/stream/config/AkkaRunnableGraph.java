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

package uk.gov.gchq.palisade.service.topicoffset.stream.config;

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

import uk.gov.gchq.palisade.service.topicoffset.common.topicoffset.TopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetRequest;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;
import uk.gov.gchq.palisade.service.topicoffset.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.topicoffset.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.topicoffset.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.topicoffset.stream.ProducerTopicConfiguration.Topic;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.topicoffset.TopicOffsetApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {

    @Bean
    KafkaProducerService kafkaProducerService(
            final Sink<ProducerRecord<String, TopicOffsetRequest>, CompletionStage<Done>> upstreamSink,
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
            final Source<CommittableMessage<String, TopicOffsetRequest>, Control> source,
            final Sink<Envelope<String, TopicOffsetResponse, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final TopicOffsetService service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        return source
                // Create a topic offset response using the offset value of the original message
                // We want to do a Source::filter, but can't because we still need to commit *every* message to the upstream source
                // Instead, map to Optional, filtering for those that are offsets, but still keeping track of every original committableMessage (probably paired with an Optional::empty)
                .map((CommittableMessage<String, TopicOffsetRequest> committableMessage) -> {
                    Optional<TopicOffsetResponse> response = Optional.of(committableMessage.record().headers())
                            // 'Filter out' the non-offsets for the topic
                            // These messages still need to be committed to the upstream, but the downstream will be discarded through the ProducerMessage::passThrough
                            .filter(service::isOffsetForTopic)
                            // Get the offset values for offsets for this topic
                            .map(ignored -> service.createTopicOffsetResponse(committableMessage.committableOffset().partitionOffset().offset()));

                    return new Pair<>(committableMessage, response);
                })

                // Either send the Optional::present result to the downstream kafka, or discard the Optional::empty
                // Create a ProducerMessage of either a committable ::passThrough (if not an offset) or a committable ::single (if an offset)
                // In this way, *every* upstream message is committed exactly once
                .map((Pair<CommittableMessage<String, TopicOffsetRequest>, Optional<TopicOffsetResponse>> messageAndResponse) -> {
                    Committable consumerOffset = messageAndResponse.first().committableOffset();
                    return messageAndResponse.second()
                            .map((TopicOffsetResponse response) -> {
                                ConsumerRecord<String, ?> requestRecord = messageAndResponse.first().record();
                                // Build producer record, copying the partition, keeping track of original message
                                // In the future, consider recalculating the token according to number of upstream/downstream partitions available
                                ProducerRecord<String, TopicOffsetResponse> record = new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(), response, requestRecord.headers());
                                // Build producer message, applying the committable pass-thru and consuming the original message
                                return ProducerMessage.single(record, consumerOffset);
                            })
                            // We must commit the consumer (upstream) offset even if messages are 'filtered out' and discarded
                            .orElse(ProducerMessage.passThrough(consumerOffset));
                })

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }
}
