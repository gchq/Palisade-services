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

import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.Token;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.resource.ResourceApplication}
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
            final Source<CommittableMessage<String, ResourceRequest>, Control> source,
            final Sink<Envelope<String, ResourceResponse, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final StreamingResourceServiceProxy service) {
        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        // Read messages from the stream source
        return source
                // Extract token from message, keeping track of original message
                .map(committableMessage -> new Pair<>(committableMessage, new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset())))

                // Store authorised request in persistence, keeping track of original message and token
                .mapAsync(PARALLELISM, (Pair<CommittableMessage<String, ResourceRequest>, String> messageAndToken) -> {
                    ResourceRequest request = messageAndToken.first().record().value();
                    return service.getResourcesById(messageAndToken.second(), request)
                            .thenApply(ignored -> new Tuple3<>(messageAndToken.first(), messageAndToken.second(), request));
                })

                // Mask resource attributes, keeping track of original message and token
                .map(messageTokenRequest -> new Tuple3<>(messageTokenRequest.t1(), messageTokenRequest.t2(), service.maskResourceAttributes(messageTokenRequest.t3())))

                // Build producer record, copying the partition, keeping track of original message
                .map((Tuple3<CommittableMessage<String, ResourceRequest>, String, ResourceResponse> messageTokenResponse) -> {
                    ConsumerRecord<String, ResourceRequest> requestRecord = messageTokenResponse.t1().record();
                    // In the future, consider recalculating the token according to number of upstream/downstream partitions available
                    return new Pair<>(messageTokenResponse.t1(), new ProducerRecord<>(outputTopic.getName(), requestRecord.partition(), requestRecord.key(), messageTokenResponse.t3(), requestRecord.headers()));
                })

                // Build producer message, applying the committable pass-thru consuming the original message
                .map(messageAndRecord -> ProducerMessage.single(messageAndRecord.second(), (Committable) messageAndRecord.first().committableOffset()))

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }

}
