package uk.gov.gchq.palisade.service.attributemask.stream.config;

import akka.Done;
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerMessage.Envelope;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Consumer.Control;
import akka.kafka.javadsl.Consumer.DrainingControl;
import akka.stream.ActorAttributes;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;
import uk.gov.gchq.palisade.service.attributemask.web.KafkaController;

import java.util.concurrent.CompletionStage;

@Configuration
public class AkkaRunnableGraph {

    static ProducerRecord<String, AttributeMaskingResponse> recordTransformer(
            final ConsumerRecord<String, AttributeMaskingRequest> consumerRecord,
            final KafkaController controller,
            final Topic outputTopic) {
        return controller.maskAttributes(consumerRecord, outputTopic);
    }

    static Envelope<String, AttributeMaskingResponse, Committable> envelopeTransformer(
            final CommittableMessage<String, AttributeMaskingRequest> committableMessage,
            final KafkaController controller,
            final Topic outputTopic) {
        // Put the record into a committable message
        ProducerRecord<String, AttributeMaskingResponse> producerRecord = recordTransformer(committableMessage.record(), controller, outputTopic);
        return ProducerMessage.single(producerRecord, committableMessage.committableOffset());
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> runner(
            final Source<CommittableMessage<String, AttributeMaskingRequest>, Control> source,
            final Sink<Envelope<String, AttributeMaskingResponse, Committable>, CompletionStage<Done>> sink,
            final ProducerTopicConfiguration topicConfiguration,
            final Function1<Throwable, Directive> supervisor,
            final KafkaController controller) {
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");

        return source
                .map(committableMessage -> envelopeTransformer(committableMessage, controller, outputTopic))
                .withAttributes(ActorAttributes.supervisionStrategy(supervisor))
                .toMat(sink, Consumer::createDrainingControl);
    }

}
