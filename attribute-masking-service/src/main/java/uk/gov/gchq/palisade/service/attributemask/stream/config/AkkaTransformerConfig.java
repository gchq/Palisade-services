package uk.gov.gchq.palisade.service.attributemask.stream.config;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Consumer.Control;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.attributemask.stream.StreamComponents;

import java.util.concurrent.CompletionStage;

@Configuration
public class AkkaTransformerConfig {
    private static final StreamComponents<String, AttributeMaskingRequest> INPUT_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AttributeMaskingResponse> OUTPUT_COMPONENTS = new StreamComponents<>();

    @Bean
    Source<CommittableMessage<String, AttributeMaskingRequest>, Control> committableRequestSource(final ActorSystem actorSystem, final ConsumerTopicConfiguration configuration) {
        ConsumerSettings<String, AttributeMaskingRequest> consumerSettings = INPUT_COMPONENTS.consumerSettings(
                actorSystem,
                SerDesConfig.keyDeserializer(),
                SerDesConfig.valueDeserializer());

        return INPUT_COMPONENTS.committableConsumer(consumerSettings, configuration.getTopics().get("input-topic").getName());
    }

    @Bean
    Sink<Envelope<String, AttributeMaskingResponse, Committable>, CompletionStage<Done>> committableResponseSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AttributeMaskingResponse> producerSettings = OUTPUT_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.keySerializer(),
                SerDesConfig.valueSerializer());

        CommitterSettings committerSettings = OUTPUT_COMPONENTS.committerSettings(actorSystem);
        return OUTPUT_COMPONENTS.committableProducer(producerSettings, committerSettings);
    }
}
