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

import uk.gov.gchq.palisade.service.attributemask.exception.AuditableException;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.attributemask.web.KafkaController;

import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    static Envelope<String, AttributeMaskingResponse, Committable> committableConverter(
            final CommittableMessage<String, AttributeMaskingRequest> committableMessage,
            final KafkaController controller,
            final Topic outputTopic) {
        // Pull the record out of the committable message
        ConsumerRecord<String, AttributeMaskingRequest> consumerRecord = committableMessage.record();
        // Process the record with the service
        ProducerRecord<String, AttributeMaskingResponse> producerRecord = controller.maskAttributes(consumerRecord, outputTopic);
        // Put the record into a committable message, with a matching committer
        return ProducerMessage.single(producerRecord, committableMessage.committableOffset());
    }

    static Function1<Throwable, Directive> auditSupervisor(
            final Materializer materializer,
            final KafkaController controller,
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> errorSink,
            final Topic errorTopic) {
        return (Throwable ex) -> {
            LOGGER.warn("Received {} exception, supervising now", ex.getClass());
            if (ex instanceof AuditableException) {
                try {
                    LOGGER.info("Exception was auditable with cause {}, auditing and resuming", ex.getCause().getClass());
                    Source.single(controller.<String>auditError((AuditableException) ex, errorTopic))
                            .runWith(errorSink, materializer);
                    return Supervision.resumingDecider().apply(ex);
                } catch (RuntimeException e) {
                    LOGGER.error("Error occurred while auditing, stopping now!", e);
                    return Supervision.stoppingDecider().apply(e);
                }
            } else {
                LOGGER.warn("Exception {} was not auditable, attempting to resume", ex.getClass());
                return Supervision.resumingDecider().apply(ex);
            }
        };
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> runner(
            final Materializer materializer,
            final Source<CommittableMessage<String, AttributeMaskingRequest>, Control> source,
            final Sink<Envelope<String, AttributeMaskingResponse, Committable>, CompletionStage<Done>> sink,
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> errorSink,
            final ProducerTopicConfiguration topicConfiguration,
            final KafkaController controller) {
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        return source
                .map(committableMessage -> committableConverter(committableMessage, controller, outputTopic))
                .withAttributes(ActorAttributes.supervisionStrategy(auditSupervisor(materializer, controller, errorSink, errorTopic)))
                .toMat(sink, Consumer::createDrainingControl);
    }

}
