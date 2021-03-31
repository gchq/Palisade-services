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

package uk.gov.gchq.palisade.service.attributemask.web;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.common.Token;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.service.KafkaProducerService;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class AttributeMaskingRestControllerTest {

    private LinkedList<ProducerRecord<String, AttributeMaskingRequest>> sinkAggregation = new LinkedList<>();
    private final Sink<ProducerRecord<String, AttributeMaskingRequest>, CompletionStage<Done>> aggregatorSink = Sink.foreach(sinkAggregation::addLast);
    private final ConsumerTopicConfiguration mockTopicConfig = Mockito.mock(ConsumerTopicConfiguration.class);
    private final Topic mockTopic = Mockito.mock(Topic.class);
    private final Materializer materializer = Materializer.createMaterializer(ActorSystem.create());

    @AfterEach
    void tearDown() {
        Mockito.reset(mockTopicConfig, mockTopic);
        sinkAggregation = new LinkedList<>();
    }

    @Test
    void testControllerDelegatesToAkkaSink() {
        // given some test data, and a mocked service behind the controller
        AttributeMaskingRestController attributeMaskingRestController = new AttributeMaskingRestController(
                new KafkaProducerService(aggregatorSink, mockTopicConfig, materializer)
        );
        Mockito.when(mockTopicConfig.getTopics()).thenReturn(Collections.singletonMap("input-topic", mockTopic));
        Mockito.when(mockTopic.getName()).thenReturn("upstream-topic");
        Mockito.when(mockTopic.getPartitions()).thenReturn(1);

        // when the controller is called with a request
        Map<String, String> headers = Collections.singletonMap(Token.HEADER, ApplicationTestData.REQUEST_TOKEN);
        attributeMaskingRestController.maskAttributes(headers, ApplicationTestData.REQUEST);

        // Then the sink aggregated the request
        assertThat(sinkAggregation)
                .hasSize(1)
                .first().satisfies(record -> assertAll("Test Record Headers and Value",
                () -> assertThat(record)
                        .extracting(ProducerRecord::value)
                        .isEqualTo(ApplicationTestData.REQUEST),

                () -> assertThat(record)
                        .extracting(ProducerRecord::headers)
                        .extracting(kHeaders -> kHeaders.lastHeader(Token.HEADER).value())
                        .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes())
                )
        );
    }

    @Test
    void testControllerAcceptsNulls() {
        // given some test data, and a mocked service behind the controller
        AttributeMaskingRestController attributeMaskingRestController = new AttributeMaskingRestController(
                new KafkaProducerService(aggregatorSink, mockTopicConfig, materializer)
        );
        Mockito.when(mockTopicConfig.getTopics()).thenReturn(Collections.singletonMap("input-topic", mockTopic));
        Mockito.when(mockTopic.getName()).thenReturn("upstream-topic");
        Mockito.when(mockTopic.getPartitions()).thenReturn(1);

        // when the controller is called with a request
        Map<String, String> headers = Collections.singletonMap(Token.HEADER, ApplicationTestData.REQUEST_TOKEN);
        attributeMaskingRestController.maskAttributes(headers, null);

        // Then the sink aggregated the request
        assertThat(sinkAggregation)
                .hasSize(1)
                .first().satisfies(record -> assertAll("Test Record Headers and Value",
                () -> assertThat(record)
                        .extracting(ProducerRecord::value)
                        .isNull(),

                () -> assertThat(record)
                        .extracting(ProducerRecord::headers)
                        .extracting(kHeaders -> kHeaders.lastHeader(Token.HEADER).value())
                        .isEqualTo(ApplicationTestData.REQUEST_TOKEN.getBytes())
                )
        );
    }

}
