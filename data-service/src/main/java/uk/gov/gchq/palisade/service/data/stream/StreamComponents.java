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

package uk.gov.gchq.palisade.service.data.stream;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.DiscoverySupport;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    /**
     * Construct an Akka Kafka ProducerSettings from the given config and serialisers
     *
     * @param system          the application's actor system used to load config values
     * @param keySerializer   the stream's key serialiser
     * @param valueSerializer the stream's value serialiser
     * @return a {@link ProducerSettings} object for creating Akka {@link Sink}s
     */
    public ProducerSettings<K, V> producerSettings(final ActorSystem system, final Serializer<K> keySerializer, final Serializer<V> valueSerializer) {
        Config config = system.settings().config().getConfig("akka.kafka.producer");
        return ProducerSettings.create(config, keySerializer, valueSerializer)
                .withEnrichCompletionStage(DiscoverySupport.producerBootstrapServers(config, system));
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
}
