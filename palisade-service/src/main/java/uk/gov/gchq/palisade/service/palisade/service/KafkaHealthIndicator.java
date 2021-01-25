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

package uk.gov.gchq.palisade.service.palisade.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
    private static final String TOPIC_NAME = "request";
    private final AdminClient adminClient;

    /**
     * Requires the AdminClient to interact with Kafka
     *
     * @param adminClient of the cluster
     */
    public KafkaHealthIndicator(final AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public Health getHealth(final boolean includeDetails) {
        return Optional.of(performCheck())
                .filter(healthy -> healthy)
                .map(up -> Health.up().withDetail("topic", TOPIC_NAME).build())
                .orElseGet(() -> Health.down().withDetail("topic", TOPIC_NAME).build());
    }

    @Override
    public Health health() {
        return Optional.of(performCheck())
                .filter(healthy -> healthy)
                .map(up -> Health.up().build())
                .orElseGet(() -> Health.down().build());
    }

    private boolean performCheck() {
        try {

            Map<String, TopicDescription> topicsResult = adminClient.describeTopics(Collections.singleton(TOPIC_NAME)).all().get(1, TimeUnit.SECONDS);
            return topicsResult.get(TOPIC_NAME).name().equals(TOPIC_NAME);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Timeout during Kafka health check for group {}", TOPIC_NAME, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
