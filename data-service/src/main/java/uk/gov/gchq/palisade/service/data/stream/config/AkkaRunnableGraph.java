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
package uk.gov.gchq.palisade.service.data.stream.config;


import akka.Done;
import akka.NotUsed;
import akka.stream.Supervision;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import scala.Function1;

import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;
import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration.Topic;

import java.util.concurrent.CompletionStage;

/**
 * Configuration for the Akka Runnable Graph used by the data-servic for the connection between Kafka, Akka and the
 * service
 */
public class AkkaRunnableGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaRunnableGraph.class);

    @Bean
    Function1<Throwable, Supervision.Directive> supervisor() {
        return ex -> Supervision.resumingDecider().apply(ex);
    }

    @Bean
    RunnableGraph<Sink<AuditableDataReaderResponse, NotUsed>> runner(
            final Source<AuditableDataReaderResponse, Sink<AuditableDataReaderResponse, NotUsed>> source,
            final Sink<ProducerRecord<String, byte[]>, CompletionStage<Done>> sink,
            final Function1<Throwable, Supervision.Directive> supervisionStrategy,
            final ProducerTopicConfiguration topicConfiguration) {

        // Get output topic from config
        Topic outputTopic = topicConfiguration.getTopics().get("output-topic");
        // Get error topic from config
        Topic errorTopic = topicConfiguration.getTopics().get("error-topic");

        return null;
    }
}
/*
        return source
                .flatMapConcat((AuditableDataReaderResponse auditableDataReaderResponse) -> {

                    //decidePartition
                    String token = auditableDataReaderResponse.getToken();
                    Integer partition = Token.toPartition(auditableDataReaderResponse.getToken(), outputTopic.getPartitions());

                    //
                    BiFunction<AuditableDataReaderResponse, Headers, ProducerRecord<String, byte[]>> recordFunc = (AuditableDataReaderResponse value, Headers headers) -> {
                        // Make the AuditablePalisadeRequest an Optional

                        // Found and error audit message to send
                        /*
                        Optional.ofNullable(auditableDataReaderResponse.getAuditErrorMessage())
                                .map(audit -> ProducerMessage.single(
                                        new ProducerRecord<>(errorTopic.getName(), partition, null,
                                                SerDesConfig.errorValueSerializer().serialize(null, audit), requestRecord.headers()),
                                        committable))


                                .map( new ProducerRecord<>(errorTopic.getName(), partition, (String) null,
                                SerDesConfig.errorValueSerializer().serialize(null, auditableDataReaderResponse.getAuditErrorMessage()), headers))
                                .orElse(new ProducerRecord<>(outputTopic.getName(), partition, (String) null,
                                        SerDesConfig.successValueSerializer().serialize(null,
                                                auditableDataReaderResponse.getAuditSuccessMessage(), headers)):

                         */

                        //create Headers for body
                      //  Headers requestHeaders = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, token.getBytes(Charset.defaultCharset()))});

                     //   LOGGER.debug("token {} and request: {}", token, auditableDataReaderResponse);
                        // return Source.from(List.of(
                        //  recordFunc.apply(tokenAndRequest.second(), requestHeaders); // Body

                        //  )
                        // Send errors to supervisor
                        // .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

                        // Send messages to the sink
                        //  .to(sink);
                       // return null;
                   // };
//}}
             //   };
  //  }
//}
