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

package uk.gov.gchq.palisade.service.filteredresource.stream.config;

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

import uk.gov.gchq.palisade.service.filteredresource.common.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.filteredresource.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.filteredresource.stream.StreamComponents;

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
    private static final StreamComponents<String, FilteredResourceRequest> INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, TopicOffsetMessage> OFFSET_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditErrorMessage> ERROR_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditSuccessMessage> SUCCESS_COMPONENTS = new StreamComponents<>();

    /**
     * Create a new Committable Source dynamically given a topic offset and partition.
     * The likely use-case is groupId=[client token], topicPartition=[hash of token] topicOffset=[from topic-offset-service]
     *
     * @param <K> source's kafka topic key type
     * @param <V> source's kafka topic value type
     */
    public interface PartitionedOffsetSourceFactory<K, V> {
        /**
         * Create a new Committable Source dynamically given a topic offset and partition.
         * The likely use-case is groupId=[client token], topicPartition=[hash of token] topicOffset=[from topic-offset-service]
         *
         * @param token  the client's token for this request, which is used for the consumer group-id and partition selection
         * @param offset the offset to start with for the given token
         * @return a new Kafka source
         * @implNote the offset should come from the {@link uk.gov.gchq.palisade.service.filteredresource.repository.offset.TokenOffsetController}
         * to ensure it is accurate (i.e. it points to the start-of-stream message)
         */
        Source<CommittableMessage<K, V>, Control> create(String token, Long offset);
    }

    @Bean
    PartitionedOffsetSourceFactory<String, FilteredResourceRequest> committableRequestSourceFactory(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        Topic topic = configuration.getTopics().get("input-topic");
        ConsumerSettings<String, FilteredResourceRequest> consumerSettings = INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.maskedResourceKeyDeserializer(),
                SerDesConfig.maskedResourceValueDeserializer());

        return (String token, Long offset) -> {
            // Convert the token to a partition number
            int partition = Token.toPartition(token, topic.getPartitions());
            // Dynamically create partition/offset subscription (based on client token)
            Subscription subscription = Subscriptions.assignmentWithOffset(new TopicPartition(topic.getName(), partition), offset);
            // Create new kafka consumer / akka source
            // The use of the token as the groupId could be used for better client error recovery, but this isn't actually done
            // Instead it is just used as a convenient unique groupId for this kafka consumer
            return INPUT_COMPONENTS.committableConsumer(consumerSettings.withGroupId(token), subscription);
        };
    }

    @Bean
    Source<CommittableMessage<String, TopicOffsetMessage>, Control> committableOffsetSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        Topic topic = configuration.getTopics().get("offset-topic");
        ConsumerSettings<String, TopicOffsetMessage> consumerSettings = OFFSET_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.maskedResourceOffsetKeyDeserializer(),
                SerDesConfig.maskedResourceOffsetValueDeserializer());
        Subscription subscription = Optional.ofNullable(topic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(topic.getName(), partition)))
                .orElse(Subscriptions.topics(topic.getName()));

        return OFFSET_COMPONENTS.committableConsumer(consumerSettings, subscription);
    }

    @Bean
    Source<CommittableMessage<String, AuditErrorMessage>, Control> committableAuditErrorMessageSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        Topic topic = configuration.getTopics().get("error-topic");
        ConsumerSettings<String, AuditErrorMessage> consumerSettings = ERROR_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.errorKeyDeserializer(),
                SerDesConfig.errorValueDeserializer());
        Subscription subscription = Optional.ofNullable(topic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(topic.getName(), partition)))
                .orElse(Subscriptions.topics(topic.getName()));

        return ERROR_COMPONENTS.committableConsumer(consumerSettings, subscription);
    }

    @Bean
    Sink<Committable, CompletionStage<Done>> offsetCommitterSink(final ActorSystem actorSystem) {
        CommitterSettings committerSettings = OFFSET_COMPONENTS.committerSettings(actorSystem);

        return OFFSET_COMPONENTS.committerSink(committerSettings);
    }

    @Bean
    Sink<Envelope<String, AuditSuccessMessage, Committable>, CompletionStage<Done>> committableSuccessSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditSuccessMessage> producerSettings = SUCCESS_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.successKeySerializer(),
                SerDesConfig.successValueSerializer());
        CommitterSettings committerSettings = SUCCESS_COMPONENTS.committerSettings(actorSystem);

        return SUCCESS_COMPONENTS.committableProducer(producerSettings, committerSettings);
    }

    @Bean
    Sink<ProducerRecord<String, FilteredResourceRequest>, CompletionStage<Done>> plainRequestSink(final ActorSystem actorSystem) {
        ProducerSettings<String, FilteredResourceRequest> producerSettings = INPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.maskedResourceKeySerializer(),
                SerDesConfig.maskedResourceValueSerializer());

        return INPUT_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    Sink<ProducerRecord<String, TopicOffsetMessage>, CompletionStage<Done>> plainOffsetSink(final ActorSystem actorSystem) {
        ProducerSettings<String, TopicOffsetMessage> producerSettings = OFFSET_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.maskedResourceOffsetKeySerializer(),
                SerDesConfig.maskedResourceOffsetValueSerializer());

        return OFFSET_COMPONENTS.plainProducer(producerSettings);
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
