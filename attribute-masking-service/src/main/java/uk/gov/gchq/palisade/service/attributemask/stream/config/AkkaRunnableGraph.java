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

import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.model.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication}
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
            final Source<CommittableMessage<String, AttributeMaskingRequest>, Control> source,
            final Sink<Envelope<String, AttributeMaskingResponse, Committable>, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration,
            final AttributeMaskingService service) {

        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        // Read messages from the stream source
        return source
                // Extract token from message
                .map(committableMessage -> new Pair<>(committableMessage, new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset())))
                // Store authorised request in persistence
                .mapAsync(PARALLELISM, (Pair<CommittableMessage<String, AttributeMaskingRequest>, String> messageAndToken) -> {
                    AttributeMaskingRequest request = messageAndToken.first().record().value();
                    return service.storeAuthorisedRequest(messageAndToken.second(), request)
                            .thenApply(ignored -> new Pair<>(messageAndToken.first(), request));
                })
                // Mask resource attributes
                .map(messageAndRequest -> new Pair<>(messageAndRequest.first(), service.maskResourceAttributes(messageAndRequest.second())))
                // Build producer record
                .map((Pair<CommittableMessage<String, AttributeMaskingRequest>, AttributeMaskingResponse> messageAndResponse) -> {
                    ConsumerRecord<String, AttributeMaskingRequest> record = messageAndResponse.first().record();
                    return new Pair<>(messageAndResponse.first(), new ProducerRecord<>(outputTopic.getName(), record.partition(), record.key(), messageAndResponse.second(), record.headers()));
                })
                // Build producer message (with committable pass-thru)
                .map(messageAndRecord -> ProducerMessage.single(messageAndRecord.second(), (Committable) messageAndRecord.first().committableOffset()))
                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }

}
