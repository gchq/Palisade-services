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

package uk.gov.gchq.palisade.service.audit.stream.config;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ConsumerSettings;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.audit.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.audit.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.audit.stream.StreamComponents;

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
@EnableConfigurationProperties(AuditServiceConfigProperties.class)
public class AkkaComponentsConfig {

    private static final StreamComponents<String, AuditSuccessMessage> SUCCESS_INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditErrorMessage> ERROR_INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<Committable, CompletionStage<Done>> OUTPUT_COMPONENTS = new StreamComponents<>();

    private final AuditServiceConfigProperties configProperties;

    /**
     * Public constructor for the {@link AkkaComponentsConfig}
     *
     * @param configProperties configuration details for the Audit Service
     */
    public AkkaComponentsConfig(final AuditServiceConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @Bean
    Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> successRequestSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditSuccessMessage> producerSettings = SUCCESS_INPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.successKeySerialiser(),
                SerDesConfig.successValueSerialiser()
        );

        return SUCCESS_INPUT_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    Source<CommittableMessage<String, AuditSuccessMessage>, Control> successCommittableRequestSource(final ActorSystem actorSystem,
                                                                                                     final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, AuditSuccessMessage> consumerSettings = SUCCESS_INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.successKeyDeserialiser(),
                SerDesConfig.successValueDeserialiser(configProperties));

        Topic successTopic = configuration.getTopic("success-topic");
        Subscription successSubscription = Optional.ofNullable(successTopic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(successTopic.getName(), partition)))
                .orElse(Subscriptions.topics(successTopic.getName()));

        return SUCCESS_INPUT_COMPONENTS.committableConsumer(consumerSettings, successSubscription);
    }

    @Bean
    CommitterSettings successCommitterSettings(final ActorSystem actorSystem) {
        return SUCCESS_INPUT_COMPONENTS.committerSettings(actorSystem);
    }

    @Bean
    Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> errorRequestSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditErrorMessage> producerSettings = ERROR_INPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.errorKeySerialiser(),
                SerDesConfig.errorValueSerialiser()
        );

        return ERROR_INPUT_COMPONENTS.plainProducer(producerSettings);
    }

    @SuppressWarnings("resource")
    @Bean
    Source<CommittableMessage<String, AuditErrorMessage>, Control> errorCommittableRequestSource(final ActorSystem actorSystem,
                                                                                                 final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ERROR_INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.errorKeyDeserialiser(),
                SerDesConfig.errorValueDeserialiser(configProperties));

        Topic errorTopic = configuration.getTopic("error-topic");
        Subscription errorSubscription = Optional.ofNullable(errorTopic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(errorTopic.getName(), partition)))
                .orElse(Subscriptions.topics(errorTopic.getName()));

        return ERROR_INPUT_COMPONENTS.committableConsumer(consumerSettings, errorSubscription);
    }

    @Bean
    CommitterSettings errorCommitterSettings(final ActorSystem actorSystem) {
        return ERROR_INPUT_COMPONENTS.committerSettings(actorSystem);
    }

    @Bean
    Sink<Committable, CompletionStage<Done>> committableSink(final ActorSystem actorSystem) {
        return OUTPUT_COMPONENTS.committableSink(OUTPUT_COMPONENTS.committerSettings(actorSystem));
    }

    @Bean
    AdminClient adminClient(final ActorSystem actorSystem) {
        final List<? extends Config> servers = actorSystem.settings().config().getConfigList("akka.discovery.config.services.kafka.endpoints");
        final String bootstrap = servers.stream()
                .map(config -> String.format("%s:%d", config.getString("host"), config.getInt("port")))
                .collect(Collectors.joining(","));
        return AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, bootstrap));
    }
}
