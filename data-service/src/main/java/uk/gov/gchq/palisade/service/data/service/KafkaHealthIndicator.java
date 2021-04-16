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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.data.stream.ProducerTopicConfiguration;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Kafka health indicator.  Check that the consumer group can be accessed and is registered with the cluster,
 * if not mark the service as unhealthy.
 */
@Component
@ConditionalOnEnabledHealthIndicator("kafka")
public class KafkaHealthIndicator implements HealthIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    private final AdminClient adminClient;
    private final ProducerTopicConfiguration topicConfiguration;

    /**
     * Requires the AdminClient to interact with Kafka.
     *
     * @param adminClient        of the cluster
     * @param topicConfiguration contains the producer topic configuration
     */
    public KafkaHealthIndicator(final AdminClient adminClient, final ProducerTopicConfiguration topicConfiguration) {
        this.adminClient = adminClient;
        this.topicConfiguration = topicConfiguration;
    }

    @Override
    public Health health() {
        Set<String> configTopics = topicConfiguration.getTopicNames();
        Set<String> kafkaTopics = topicsFromKafka(adminClient.describeTopics(configTopics));

        if (kafkaTopics.equals(configTopics)) {
            return Health.up()
                    .withDetail("topics", configTopics)
                    .build();
        } else {
            return Health.down()
                    .withDetail("configTopics", configTopics)
                    .withDetail("kafkaTopics", kafkaTopics)
                    .build();
        }
    }

    private static Set<String> topicsFromKafka(final DescribeTopicsResult topicsResult) {
        try {
            // Get topic names registered with kafka
            return topicsResult.all()
                    .get(1, TimeUnit.SECONDS)
                    .values()
                    .stream()
                    .map(TopicDescription::name)
                    .collect(Collectors.toSet());
        } catch (InterruptedException e) {
            LOGGER.warn("Await on future interrupted", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.warn("Execution exception when completing kafka future", e);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout connecting to kafka", e);
        }
        // After logging any errors, return empty set
        // The service will keep running, but appear unhealthy
        return Collections.emptySet();
    }
}
