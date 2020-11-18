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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.filteredresource.model.FilteredResourceRequest;
import uk.gov.gchq.palisade.service.filteredresource.model.Token;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.filteredresource.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.filteredresource.stream.StreamComponents;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Configuration for all kafka connections for the application
 */
@Configuration
public class AkkaComponentsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaComponentsConfig.class);
    private static final int GRACE_PERIOD_MILLIS = 5000;
    private static final StreamComponents<String, FilteredResourceRequest> INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, TopicOffsetMessage> OFFSET_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditErrorMessage> ERROR_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditSuccessMessage> SUCCESS_COMPONENTS = new StreamComponents<>();

    public interface PartitionedOffsetSourceFactory<K, V> {

        /**
         * Create a new Committable Source dynamically given a topic offset and partition.
         * The likely use-case is groupId=[client token], topicPartition=[hash of token] topicOffset=[from topic-offset-service]
         *
         * @param token          the client's token for this request, which is used for the consumer group-id and partition selection
         * @param offsetFunction a function that supplies the offset to start with for the given token, defaulting to 'now' if empty
         * @return a new Kafka source
         * @implNote it is important that the offsetFunction's future truly is started on the {@link Supplier#get()} method call as
         * we need to ensure an accurate time for 'now' before starting the method call
         */
        Source<CommittableMessage<K, V>, Control> create(String token, Function<String, CompletableFuture<Optional<Long>>> offsetFunction);

    }

    @Bean
    PartitionedOffsetSourceFactory<String, FilteredResourceRequest> committableRequestSourceFactory(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, FilteredResourceRequest> consumerSettings = INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.maskedResourceKeyDeserializer(),
                SerDesConfig.maskedResourceValueDeserializer());
        Topic topic = configuration.getTopics().get("input-topic");

        return (String token, Function<String, CompletableFuture<Optional<Long>>> offsetSupplier) -> {
            // Set the default offset if one is not found in persistence
            // n.b. There is the chance for a data race between the attribute-masking-service writing a response and the
            // topic-offset-service processing it and writing its response - as such, there needs to be some grace period
            // such that this 'now' is actually 'now - 5000ms' or similar
            long now = System.currentTimeMillis() - GRACE_PERIOD_MILLIS;
            // Try to get an offset from persistence, in this case an error thrown is recoverable, but do still log it
            Optional<Long> topicOffset = offsetSupplier.apply(token)
                    .exceptionally(throwable -> {
                        LOGGER.warn("Failure while getting offset from persistence, using 'now' as offset", throwable);
                        return Optional.empty();
                    })
                    .join();
            // Convert the token to a partition number
            int partition = Token.toPartition(token, topic.getPartitions());
            // Dynamically create partition/offset subscription (based on client token)
            Subscription subscription = topicOffset
                    .map(storedOffset -> Subscriptions.assignmentWithOffset(new TopicPartition(topic.getName(), partition), storedOffset))
                    .orElse(Subscriptions.assignmentOffsetsForTimes(new TopicPartition(topic.getName(), partition), now));
            // Create new kafka consumer / akka source
            // The use of the token as the groupId could be used for better client error recovery, but this isn't actually done
            // Instead it is just used as a convenient unique groupId for this kafka consumer
            return INPUT_COMPONENTS.committableConsumer(consumerSettings.withGroupId(token), subscription);
        };
    }

    @Bean
    Source<CommittableMessage<String, TopicOffsetMessage>, Control> committableOffsetSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, TopicOffsetMessage> consumerSettings = OFFSET_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.maskedResourceOffsetKeyDeserializer(),
                SerDesConfig.maskedResourceOffsetValueDeserializer());

        Topic topic = configuration.getTopics().get("input-topic");
        Subscription subscription = Optional.ofNullable(topic.getAssignment())
                .map(partition -> (Subscription) Subscriptions.assignment(new TopicPartition(topic.getName(), partition)))
                .orElse(Subscriptions.topics(topic.getName()));

        return OFFSET_COMPONENTS.committableConsumer(consumerSettings, subscription);
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
}
