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

package uk.gov.gchq.palisade.service.attributemask.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.attributemask.stream.ProducerTopicConfiguration.Topic;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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
    private Map<String, ProducerTopicConfiguration.Topic> topics = new HashMap<>();

    @Generated
    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    @Generated
    public void setDiscoveryMethod(final String discoveryMethod) {
        requireNonNull(discoveryMethod);
        this.discoveryMethod = discoveryMethod;
    }

    @Generated
    public String getServiceName() {
        return serviceName;
    }

    @Generated
    public void setServiceName(final String serviceName) {
        requireNonNull(serviceName);
        this.serviceName = serviceName;
    }

    @Generated
    public String getUseDispatcher() {
        return useDispatcher;
    }

    @Generated
    public void setUseDispatcher(final String useDispatcher) {
        requireNonNull(useDispatcher);
        this.useDispatcher = useDispatcher;
    }

    @Generated
    public Map<String, String> getKafkaClients() {
        return kafkaClients;
    }

    @Generated
    public void setKafkaClients(final Map<String, String> kafkaClients) {
        requireNonNull(kafkaClients);
        this.kafkaClients = kafkaClients;
    }

    @Generated
    public Map<String, String> getConnectionChecker() {
        return connectionChecker;
    }

    @Generated
    public void setConnectionChecker(final Map<String, String> connectionChecker) {
        requireNonNull(connectionChecker);
        this.connectionChecker = connectionChecker;
    }

    @Generated
    public Map<String, Topic> getTopics() {
        return topics;
    }

    @Generated
    public void setTopics(final Map<String, Topic> topics) {
        requireNonNull(topics);
        this.topics = topics;
    }
}
