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

package uk.gov.gchq.palisade.service.attributemask.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka health indicator. Check that the consumer group can be accessed and is registered with the cluster,
 * if not mark the service as unhealthy.
 */
@Component("kafka")
@ConditionalOnEnabledHealthIndicator("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    @Value("${akka.kafka.consumer.kafka-clients.group.id}")
    private String groupId;

    private final AdminClient adminClient;

    public KafkaHealthIndicator(final AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public Health getHealth(final boolean includeDetails) {
        return performCheck() ? Health.up().withDetail("group", this.groupId).build() : Health.down().withDetail("group", this.groupId).build();
    }

    /**
     * Health endpoint
     * @return the {@code Health} object
     */
    @Override
    public Health health() {
        return performCheck() ? Health.up().build() : Health.down().build();
    }

    private boolean performCheck() {
        try {
            Map<String, ConsumerGroupDescription> groupDescriptionMap = this.adminClient.describeConsumerGroups(Collections.singletonList(this.groupId))
                            .all()
                            .get(1, TimeUnit.SECONDS);

            ConsumerGroupDescription consumerGroupDescription = groupDescriptionMap.get(this.groupId);

            LOGGER.debug("Kafka consumer group ({}) state: {}", groupId, consumerGroupDescription.state());

            if (consumerGroupDescription.state().equals(ConsumerGroupState.STABLE)) {
                return consumerGroupDescription.members().stream()
                        .noneMatch(member -> (member.assignment() == null || member.assignment().topicPartitions().isEmpty()));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }

        return true;
    }
}
