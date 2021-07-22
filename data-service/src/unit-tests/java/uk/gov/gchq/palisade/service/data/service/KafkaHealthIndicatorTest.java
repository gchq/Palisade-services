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

package uk.gov.gchq.palisade.service.data.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHealthIndicatorTest {
    AdminClient adminClient = Mockito.mock(AdminClient.class);
    ProducerTopicConfiguration producerTopicConfiguration = Mockito.mock(ProducerTopicConfiguration.class);
    KafkaHealthIndicator indicator = new KafkaHealthIndicator(adminClient, producerTopicConfiguration);

    @BeforeEach
    void setUp() {
        Mockito.reset(adminClient, producerTopicConfiguration);
    }

    @Test
    void testHealthWithNoTopics() {
        // Given there are no topics in config and none in kafka
        var describeTopicsResult = Mockito.mock(DescribeTopicsResult.class);
        Mockito.when(describeTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        Mockito.when(adminClient.describeTopics(Mockito.any())).thenReturn(describeTopicsResult);
        Mockito.when(producerTopicConfiguration.getTopics()).thenReturn(Map.of());

        // When we get the health from the indicator
        var health = indicator.health();

        // Then the service should be healthy (all topics in config were found in kafka)
        assertThat(health)
                .extracting(Health::getStatus)
                .as("The mock should produce no topics, thus all topics are available, thus service is healthy")
                .isEqualTo(Status.UP);
    }

    @Test
    void testHealthWithConfigTopics() {
        // Given there is one topic in config and none in kafka
        var describeTopicsResult = Mockito.mock(DescribeTopicsResult.class);
        var configTopic = new ProducerTopicConfiguration.Topic();
        configTopic.setName("testtopic");
        var configTopics = Map.of("testtopic", configTopic);
        Mockito.when(describeTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        Mockito.when(adminClient.describeTopics(Mockito.any())).thenReturn(describeTopicsResult);
        Mockito.when(producerTopicConfiguration.getTopics()).thenReturn(configTopics);

        // When we get the health from the indicator
        var health = indicator.health();

        // Then the service should be unhealthy (some topics from config were not found in kafka)
        assertThat(health)
                .extracting(Health::getStatus)
                .as("The mock produces a topic that cannot be found in kafka, thus kafka is not ready, thus the service is unhealthy")
                .isEqualTo(Status.DOWN);
    }

    @Test
    void testHealthWithKafkaAndConfigTopics() {
        // Given there are no topics in config and none in kafka
        var describeTopicsResult = Mockito.mock(DescribeTopicsResult.class);
        var kafkaTopics = Map.of("testtopic", new TopicDescription("testtopic", false, List.of()));
        var configTopic = new ProducerTopicConfiguration.Topic();
        configTopic.setName("testtopic");
        var configTopics = Map.of("testtopic", configTopic);
        Mockito.when(describeTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(kafkaTopics));
        Mockito.when(adminClient.describeTopics(Mockito.any())).thenReturn(describeTopicsResult);
        Mockito.when(producerTopicConfiguration.getTopics()).thenReturn(configTopics);

        // When we get the health from the indicator
        var health = indicator.health();

        // Then the service should be healthy (all topics in config were found in kafka)
        assertThat(health)
                .extracting(Health::getStatus)
                .as("The mock produces a number of topics, all of which can be found in kafka, thus the service is healthy")
                .isEqualTo(Status.UP);
    }
}