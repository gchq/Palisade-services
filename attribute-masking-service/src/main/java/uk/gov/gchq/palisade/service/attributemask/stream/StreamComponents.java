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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.concurrent.CompletionStage;

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

    public Source<ConsumerRecord<K, V>, Consumer.Control> plainConsumer(
            final ConsumerSettings<K, V> consumerSettings, String topic) {
        return Consumer.plainSource(consumerSettings, Subscriptions.topics(topic));
    }

}
