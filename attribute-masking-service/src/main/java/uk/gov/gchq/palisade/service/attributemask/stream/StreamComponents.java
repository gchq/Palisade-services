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

package uk.gov.gchq.palisade.service.attributemask.stream;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
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
 * These have many different options that may be used (committable, offset-able, partitioned, manual, external, etc...)
 *
 * @param <K> generic Key type
 * @param <V> generic Value type
 */
public class StreamComponents<K, V> {

    public ProducerSettings<K, V> producerSettings(final ActorSystem system, final Serializer<K> keySerializer, final Serializer<V> valueSerializer) {
        Config config = system.settings().config().getConfig("akka.kafka.producer");
        return ProducerSettings.create(config, keySerializer, valueSerializer)
                .withEnrichCompletionStage(DiscoverySupport.producerBootstrapServers(config, system));
    }

    public ConsumerSettings<K, V> consumerSettings(final ActorSystem system, final Deserializer<K> keyDeserializer, final Deserializer<V> valueDeserializer) {
        Config config = system.settings().config().getConfig("akka.kafka.consumer");
        return ConsumerSettings.create(config, keyDeserializer, valueDeserializer)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .withEnrichCompletionStage(DiscoverySupport.consumerBootstrapServers(config, system));
    }

    public CommitterSettings committerSettings(final ActorSystem system) {
        Config config = system.settings().config().getConfig("akka.kafka.committer");
        return CommitterSettings.create(config);
    }

    public Sink<ProducerMessage.Envelope<K, V, ConsumerMessage.Committable>, CompletionStage<Done>> committableProducer(
            final ProducerSettings<K, V> producerSettings,
            final CommitterSettings committerSettings) {
        return Producer.committableSink(producerSettings, committerSettings);
    }

    public Sink<ProducerRecord<K, V>, CompletionStage<Done>> plainProducer(
            final ProducerSettings<K, V> producerSettings) {
        return Producer.plainSink(producerSettings);
    }

    public Source<ConsumerMessage.CommittableMessage<K, V>, Consumer.Control> committableConsumer(
            final ConsumerSettings<K, V> consumerSettings,
            final String topic) {
        return Consumer.committableSource(consumerSettings, Subscriptions.topics(topic));
    }

}
