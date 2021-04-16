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
import akka.kafka.ConsumerMessage.Committable;
import akka.kafka.ConsumerMessage.CommittableMessage;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Consumer.Control;
import akka.kafka.javadsl.Consumer.DrainingControl;
import akka.stream.ActorAttributes;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.Supervision.Directive;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.common.Token;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.service.AuditServiceAsyncProxy;
import uk.gov.gchq.palisade.service.audit.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.audit.stream.ConsumerTopicConfiguration;

import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the {@link AuditApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final int PARALLELISM = 1;

    @Bean
    KafkaProducerService kafkaProducerService(final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> errorSink,
                                              final Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> successSink,
                                              final ConsumerTopicConfiguration upstreamConfig,
                                              final Materializer materializer) {
        return new KafkaProducerService(errorSink, successSink, upstreamConfig, materializer);
    }

    @Bean
    Function1<Throwable, Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> errorRunner(
            final Source<CommittableMessage<String, AuditErrorMessage>, Control> source,
            final Sink<Committable, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final AuditServiceAsyncProxy service) {

        // Read messages from the stream source
        return source
                // Audit the error message
                .mapAsync(PARALLELISM, (CommittableMessage<String, AuditErrorMessage> committableMessage) -> {
                    String token = new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset());
                    return service.audit(token, committableMessage.record().value())
                            .<Committable>thenApply(ignored -> committableMessage.committableOffset());
                })

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the sink
                .toMat(sink, Consumer::createDrainingControl);
    }

    @Bean
    RunnableGraph<DrainingControl<Done>> successRunner(
            final Source<CommittableMessage<String, AuditSuccessMessage>, Control> source,
            final Sink<Committable, CompletionStage<Done>> sink,
            final Function1<Throwable, Directive> supervisionStrategy,
            final AuditServiceAsyncProxy service) {

        // Read messages from the stream source
        return source
                // Audit the success message
                .mapAsync(PARALLELISM, (CommittableMessage<String, AuditSuccessMessage> committableMessage) -> {
                    String token = new String(committableMessage.record().headers().lastHeader(Token.HEADER).value(), Charset.defaultCharset());
                    return service.audit(token, committableMessage.record().value())
                            .<Committable>thenApply(ignored -> committableMessage.committableOffset());
                })

                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Materialize the stream, sending messages to the committer sink
                .toMat(sink, Consumer::createDrainingControl);
    }

}
