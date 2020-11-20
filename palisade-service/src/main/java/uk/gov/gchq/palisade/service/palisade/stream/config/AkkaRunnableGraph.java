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
package uk.gov.gchq.palisade.service.palisade.stream.config;

import akka.Done;
import akka.NotUsed;
import akka.stream.ActorAttributes;
import akka.stream.Supervision;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import scala.Function1;

import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;
import uk.gov.gchq.palisade.service.palisade.model.StreamMarker;
import uk.gov.gchq.palisade.service.palisade.model.Token;
import uk.gov.gchq.palisade.service.palisade.model.TokenRequestPair;
import uk.gov.gchq.palisade.service.palisade.stream.ProducerTopicConfiguration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * Configuration for the Akka Runnable Graph used by the {@link uk.gov.gchq.palisade.service.palisade.PalisadeApplication}
 * Configures the connection between Kafka, Akka and the service
 */
@Configuration
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    @Bean
    Function1<Throwable, Supervision.Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<Sink<TokenRequestPair, NotUsed>> runner(
            final Source<TokenRequestPair, Sink<TokenRequestPair, NotUsed>> source,
            final Sink<ProducerRecord<String, PalisadeRequest>, CompletionStage<Done>> sink,
            final Function1<Throwable, Supervision.Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration) {

        // Get output topic from config
        ProducerTopicConfiguration.Topic outputTopic = topicConfiguration.getTopics().get("output-topic");


        return source
                .flatMapConcat((TokenRequestPair tokenAndRequest) -> {
                    //create the start of stream message
                    Headers startHeaders = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, tokenAndRequest.first().getBytes(Charset.defaultCharset())),
                            new RecordHeader(StreamMarker.HEADER, StreamMarker.START.toString().getBytes(Charset.defaultCharset()))});

                    //create Headers for body
                    Headers requestHeaders = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, tokenAndRequest.first().getBytes(Charset.defaultCharset()))});

                    //create the end of stream message
                    Headers endHeaders = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, tokenAndRequest.first().getBytes(Charset.defaultCharset())),
                            new RecordHeader(StreamMarker.HEADER, StreamMarker.END.toString().getBytes(Charset.defaultCharset()))});

                    //decidePartition
                    Integer partition = Token.toPartition(tokenAndRequest.first(), outputTopic.getPartitions());

                    BiFunction<PalisadeRequest, Headers, ProducerRecord<String, PalisadeRequest>> recordFunc = (value, headers) ->
                            new ProducerRecord<>(outputTopic.getName(), partition, (String) null, value, headers);

                    LOGGER.debug("token {} and request: {}", tokenAndRequest.first(), tokenAndRequest.second());
                    return Source.from(List.of(
                            recordFunc.apply(null, startHeaders), // Start of Stream Message
                            recordFunc.apply(tokenAndRequest.second(), requestHeaders), //Body
                            recordFunc.apply(null, endHeaders))); // End of Stream Message
                })
                // Send errors to supervisor
                .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                // Send messages to the sink
                .to(sink);
    }
}