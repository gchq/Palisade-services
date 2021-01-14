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

package uk.gov.gchq.palisade.service.palisade.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.palisade.stream.ProducerTopicConfiguration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka health indicator. Check that the producer group can be accessed and is registered with the cluster,
 * if not mark the service as unhealthy.
 */
@Component("kafka")
@ConditionalOnEnabledHealthIndicator("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHealthIndicator.class);
    private final AdminClient adminClient;
    private final ProducerTopicConfiguration topicConfiguration;
    private List<String> topicNames = new LinkedList<>();

    /**
     * Requires the AdminClient to interact with Kafka
     *
     * @param adminClient        of the cluster
     * @param topicConfiguration contains the producer topic configuration
     */
    public KafkaHealthIndicator(final AdminClient adminClient, final ProducerTopicConfiguration topicConfiguration) {
        this.adminClient = adminClient;
        this.topicConfiguration = topicConfiguration;
    }

    @Override
    public Health getHealth(final boolean includeDetails) {
        return Optional.of(performCheck())
                .filter(healthy -> healthy)
                .map(up -> Health.up().withDetail("topics", topicNames).build())
                .orElseGet(() -> Health.down().withDetail("topics", topicNames).build());
    }

    @Override
    public Health health() {
        return Optional.of(performCheck())
                .filter(healthy -> healthy)
                .map(up -> Health.up().build())
                .orElseGet(() -> Health.down().build());
    }

    private boolean performCheck() {
        this.topicNames = List.of(topicConfiguration.getTopics().get("output-topic").getName(), topicConfiguration.getTopics().get("error-topic").getName());
        try {
            Map<String, TopicDescription> topicsResult = adminClient.describeTopics(topicNames).all().get(1, TimeUnit.SECONDS);
            Boolean requestTopic = topicsResult.get("request").name().equals(topicNames.get(0));
            Boolean errorTopic = topicsResult.get("error").name().equals(topicNames.get(1));

            return errorTopic && requestTopic;

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Timeout during Kafka health check for group {}", topicNames, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
