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
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscription;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer.Control;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.resource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.resource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.resource.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.resource.stream.StreamComponents;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for all kafka connections for the application
 */
@Configuration
public class AkkaComponentsConfig {
    private static final StreamComponents<String, ResourceRequest> INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, ResourceResponse> OUTPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditErrorMessage> ERROR_COMPONENTS = new StreamComponents<>();

    @Bean
    Sink<ProducerRecord<String, ResourceRequest>, CompletionStage<Done>> plainRequestSink(final ActorSystem actorSystem) {
        ProducerSettings<String, ResourceRequest> producerSettings = INPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.userKeySerializer(),
                SerDesConfig.userValueSerializer());

        return INPUT_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    Source<CommittableMessage<String, ResourceRequest>, Control> committableRequestSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, ResourceRequest> consumerSettings = INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.userKeyDeserializer(),
                SerDesConfig.userValueDeserializer());

        Topic topic = configuration.getTopics().get("input-topic");
        Subscription subscription = Optional.ofNullable(topic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(topic.getName(), partition)))
                .orElse(Subscriptions.topics(topic.getName()));

        return INPUT_COMPONENTS.committableConsumer(consumerSettings, subscription);
    }

    @Bean
    Sink<Envelope<String, ResourceResponse, Committable>, CompletionStage<Done>> committableResponseSink(final ActorSystem actorSystem) {
        ProducerSettings<String, ResourceResponse> producerSettings = OUTPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.resourceKeySerializer(),
                SerDesConfig.resourceValueSerializer());

        CommitterSettings committerSettings = OUTPUT_COMPONENTS.committerSettings(actorSystem);
        return OUTPUT_COMPONENTS.committableProducer(producerSettings, committerSettings);
    }

    @Bean
    Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> plainErrorSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditErrorMessage> producerSettings = ERROR_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.errorKeySerializer(),
                SerDesConfig.errorValueSerializer());

        return ERROR_COMPONENTS.plainProducer(producerSettings);
    }
}
