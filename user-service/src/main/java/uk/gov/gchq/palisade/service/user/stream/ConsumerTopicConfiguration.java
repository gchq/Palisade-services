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

package uk.gov.gchq.palisade.service.user.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.service.user.common.Generated;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for Akka Kafka consumer
 */
@ConfigurationProperties("akka.kafka.consumer")
public class ConsumerTopicConfiguration {
    private String discoveryMethod;
    private String serviceName;
    private String useDispatcher;
    private Map<String, String> kafkaClients = new HashMap<>();
    private Map<String, String> connectionChecker = new HashMap<>();
    private Map<String, Topic> topics = new HashMap<>();

    @Generated
    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    @Generated
    public void setDiscoveryMethod(final String discoveryMethod) {
        this.discoveryMethod = Optional.ofNullable(discoveryMethod)
                .orElseThrow(() -> new IllegalArgumentException("discoveryMethod cannot be null"));
    }

    @Generated
    public String getServiceName() {
        return serviceName;
    }

    @Generated
    public void setServiceName(final String serviceName) {
        this.serviceName = Optional.ofNullable(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("serviceName cannot be null"));
    }

    @Generated
    public String getUseDispatcher() {
        return useDispatcher;
    }

    @Generated
    public void setUseDispatcher(final String useDispatcher) {
        this.useDispatcher = Optional.ofNullable(useDispatcher)
                .orElseThrow(() -> new IllegalArgumentException("useDispatcher cannot be null"));
    }

    @Generated
    public Map<String, String> getKafkaClients() {
        return kafkaClients;
    }

    @Generated
    public void setKafkaClients(final Map<String, String> kafkaClients) {
        this.kafkaClients = Optional.ofNullable(kafkaClients)
                .orElseThrow(() -> new IllegalArgumentException("kafkaClients cannot be null"));
    }

    @Generated
    public Map<String, String> getConnectionChecker() {
        return connectionChecker;
    }

    @Generated
    public void setConnectionChecker(final Map<String, String> connectionChecker) {
        this.connectionChecker = Optional.ofNullable(connectionChecker)
                .orElseThrow(() -> new IllegalArgumentException("connectionChecker cannot be null"));
    }

    @Generated
    public Map<String, Topic> getTopics() {
        return topics;
    }

    @Generated
    public void setTopics(final Map<String, Topic> topics) {
        this.topics = Optional.ofNullable(topics)
                .orElseThrow(() -> new IllegalArgumentException("topics cannot be null"));
    }
}
