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
package uk.gov.gchq.palisade.service.data.stream.config;

import akka.Done;
import akka.NotUsed;
import akka.stream.ActorAttributes;
import akka.stream.Supervision;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.Token;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration.Topic;

import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the data-service for the connection to the audit-service via a
 * Kafka stream.
 */
@Configuration
public class AkkaRunnableGraph {

    @Bean
    Function1<Throwable, Supervision.Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<Sink<TokenMessagePair, NotUsed>> runner(
            final Source<TokenMessagePair, Sink<TokenMessagePair, NotUsed>> source,
            final Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> successSink,
            final Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> errorSink,
            final Function1<Throwable, Supervision.Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration) {

        // Get success topic from config
        Topic successTopic = topicConfiguration.getTopics().get("success-topic");
        // Get error topic from config
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        return source
                .alsoTo(Flow.<TokenMessagePair>create()
                        //for AuditSuccessMessage send to the audit-service success topic
                        .filter(tokenMessagePair -> tokenMessagePair.second() instanceof AuditSuccessMessage)
                        .map((TokenMessagePair tokenMessagePair) -> {
                            Integer partition = Token.toPartition(tokenMessagePair.first(), successTopic.getPartitions());
                            Headers headers = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, tokenMessagePair.first().getBytes(Charset.defaultCharset()))});
                            return new ProducerRecord<>(successTopic.getName(), partition, (String) null, (AuditSuccessMessage) tokenMessagePair.second(), headers);
                        })
                        .to(successSink))
                .to(Flow.<TokenMessagePair>create()
                        //for AuditErrorMessage send to the audit-service error topic
                        .filter(tokenMessagePair -> tokenMessagePair.second() instanceof AuditErrorMessage)
                        .map((TokenMessagePair tokenMessagePair) -> {
                            Integer partition = Token.toPartition(tokenMessagePair.first(), errorTopic.getPartitions());
                            Headers headers = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, tokenMessagePair.first().getBytes(Charset.defaultCharset()))});
                            return new ProducerRecord<>(errorTopic.getName(), partition, (String) null, (AuditErrorMessage) tokenMessagePair.second(), headers);
                        })
                        .to(errorSink))
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy));
    }
}

