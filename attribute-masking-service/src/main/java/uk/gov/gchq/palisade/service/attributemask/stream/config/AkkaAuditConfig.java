package uk.gov.gchq.palisade.service.attributemask.stream.config;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.attributemask.stream.StreamComponents;

import java.util.concurrent.CompletionStage;

@Configuration
public class AkkaAuditConfig {
    // TODO: Implement suitable supervisor strategies
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaAuditConfig.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final StreamComponents<String, Throwable> OUTPUT_COMPONENTS = new StreamComponents<>();

    static Serializer<String> keySerializer() {
        return new StringSerializer();
    }

    static Serializer<Throwable> valueSerializer() {
        return (key, ex) -> {
            try {
                return MAPPER.writeValueAsBytes(ex);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    static Function1<Throwable, Directive> supervisor(
            Sink<ProducerRecord<String, Throwable>, CompletionStage<Done>> sink,
            Materializer materializer
    ) {
        return ex -> {
            LOGGER.error("An error occurred, supervising it now...", ex);
            try {
                Source.single(new ProducerRecord<String, Throwable>("error-topic", ex))
                        .runWith(sink, materializer)
                        .toCompletableFuture().join();
                return Supervision.resumingDecider().apply(ex);
            } catch (Exception e) {
                return Supervision.stoppingDecider().apply(ex);
            }
        };
    }

    @Bean
    public Function1<Throwable, Directive> auditingSupervisor(final Sink<ProducerRecord<String, Throwable>, CompletionStage<Done>> sink, final Materializer materializer) {
        return supervisor(sink, materializer);
    }

    @Bean
    public Sink<ProducerRecord<String, Throwable>, CompletionStage<Done>> plainErrorProducer(final ActorSystem actorSystem) {
        final ProducerSettings<String, Throwable> producerSettings = OUTPUT_COMPONENTS.producerSettings(actorSystem, keySerializer(), valueSerializer());
        return OUTPUT_COMPONENTS.plainProducer(producerSettings);
    }

}
