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

package uk.gov.gchq.palisade.service.user.stream;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscription;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.DiscoverySupport;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.concurrent.CompletionStage;

/**
 * Default boilerplate templates for creating Akka {@link Sink}s and {@link Source}s
 * These have different options that may be used (committable, offset-able, partitioned, manual, external, etc...).
 *
 * @param <K> generic Key type
 * @param <V> generic Value type
 */
public class StreamComponents<K, V> {

    /**
     * Construct an Akka Kafka ProducerSettings from the given config and serialisers
     *
     * @param system          the application's actor system used to load config values
     * @param keySerialiser   the stream's key serialiser
     * @param valueSerialiser the stream's value serialiser
     * @return a {@link ProducerSettings} object for creating Akka {@link Sink}s
     */
    public ProducerSettings<K, V> producerSettings(final ActorSystem system, final Serializer<K> keySerialiser, final Serializer<V> valueSerialiser) {
        Config config = system.settings().config().getConfig("akka.kafka.producer");
        return ProducerSettings.create(config, keySerialiser, valueSerialiser)
                .withEnrichCompletionStage(DiscoverySupport.producerBootstrapServers(config, system));
    }

    /**
     * Construct an Akka Kafka ConsumerSettings from the given config and deserialisers
     *
     * @param system            the application's actor system used to load config values
     * @param keyDeserialiser   the stream's key deserialiser
     * @param valueDeserialiser the stream's value deserialiser
     * @return a {@link ProducerSettings} object for creating Akka {@link Source}s
     */
    public ConsumerSettings<K, V> consumerSettings(final ActorSystem system, final Deserializer<K> keyDeserialiser, final Deserializer<V> valueDeserialiser) {
        Config config = system.settings().config().getConfig("akka.kafka.consumer");
        return ConsumerSettings.create(config, keyDeserialiser, valueDeserialiser)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .withEnrichCompletionStage(DiscoverySupport.consumerBootstrapServers(config, system));
    }

    /**
     * Construct an Akka Kafka CommitterSettings from the given config
     *
     * @param system the application's actor system used to load config values
     * @return a {@link CommitterSettings} object for controlling Kafka commits
     */
    public CommitterSettings committerSettings(final ActorSystem system) {
        Config config = system.settings().config().getConfig("akka.kafka.committer");
        return CommitterSettings.create(config);
    }

    /**
     * Construct a Kafka Committable Sink for Akka streams
     *
     * @param producerSettings  the producer settings for kafka
     * @param committerSettings the committer settings for kafka
     * @return a Kafka-connected Committable Sink for Akka streams
     */
    public Sink<ProducerMessage.Envelope<K, V, ConsumerMessage.Committable>, CompletionStage<Done>> committableProducer(
            final ProducerSettings<K, V> producerSettings,
            final CommitterSettings committerSettings) {
        return Producer.committableSink(producerSettings, committerSettings);
    }

    /**
     * Construct a Kafka Plain Sink for Akka streams (no control over Kafka commits)
     *
     * @param producerSettings the producer settings for kafka
     * @return a Kafka-connected Sink for Akka streams
     */
    public Sink<ProducerRecord<K, V>, CompletionStage<Done>> plainProducer(
            final ProducerSettings<K, V> producerSettings) {
        return Producer.plainSink(producerSettings);
    }

    /**
     * Construct a Kafka Committable Source for Akka streams (no need for committer settings)
     *
     * @param consumerSettings the committer settings for kafka
     * @param subscription     the topic name (and partitions) to subscribe to
     * @return a Kafka-connected Source for Akka streams
     */
    public Source<ConsumerMessage.CommittableMessage<K, V>, Consumer.Control> committableConsumer(
            final ConsumerSettings<K, V> consumerSettings,
            final Subscription subscription) {
        return Consumer.committableSource(consumerSettings, subscription);
    }

}
