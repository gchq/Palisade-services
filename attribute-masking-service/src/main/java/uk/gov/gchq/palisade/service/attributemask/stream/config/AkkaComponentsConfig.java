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
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.attributemask.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.attributemask.stream.StreamComponents;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

/**
 * Configuration for all kafka connections for the application
 */
@Configuration
public class AkkaComponentsConfig {
    private static final StreamComponents<String, AttributeMaskingRequest> INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, byte[]> OUTPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditErrorMessage> ERROR_COMPONENTS = new StreamComponents<>();

    @Bean
    Sink<ProducerRecord<String, AttributeMaskingRequest>, CompletionStage<Done>> plainRequestSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AttributeMaskingRequest> producerSettings = INPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.ruleKeySerializer(),
                SerDesConfig.ruleValueSerializer());

        return INPUT_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    Source<CommittableMessage<String, AttributeMaskingRequest>, Control> committableRequestSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, AttributeMaskingRequest> consumerSettings = INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.ruleKeyDeserializer(),
                SerDesConfig.ruleValueDeserializer());

        Topic topic = configuration.getTopics().get("input-topic");
        Subscription subscription = Optional.ofNullable(topic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(topic.getName(), partition)))
                .orElse(Subscriptions.topics(topic.getName()));

        return INPUT_COMPONENTS.committableConsumer(consumerSettings, subscription);
    }

    @Bean
    Sink<Envelope<String, byte[], Committable>, CompletionStage<Done>> committableResponseSink(final ActorSystem actorSystem) {
        ProducerSettings<String, byte[]> producerSettings = OUTPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.maskedResourceKeySerializer(),
                SerDesConfig.passthroughValueSerializer());

        CommitterSettings committerSettings = OUTPUT_COMPONENTS.committerSettings(actorSystem);
        return OUTPUT_COMPONENTS.committableProducer(producerSettings, committerSettings);
    }

    @Bean
    CommitterSettings committerSettings(final ActorSystem actorSystem) {
        return OUTPUT_COMPONENTS.committerSettings(actorSystem);
    }

    @Bean
    ProducerSettings<String, byte[]> producerSettings(final ActorSystem actorSystem) {
        return OUTPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.maskedResourceKeySerializer(),
                SerDesConfig.passthroughValueSerializer());
    }

    @Bean
    Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> plainErrorSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditErrorMessage> producerSettings = ERROR_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.errorKeySerializer(),
                SerDesConfig.errorValueSerializer());

        return ERROR_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    AdminClient adminClient(final ActorSystem actorSystem) {
        final List<? extends Config> servers = actorSystem.settings().config().getConfigList("akka.discovery.config.services.kafka.endpoints");
        final String bootstrap = servers.stream().map(config -> String.format("%s:%d", config.getString("host"), config.getInt("port"))).collect(Collectors.joining(","));

        return AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, bootstrap));
    }
}
