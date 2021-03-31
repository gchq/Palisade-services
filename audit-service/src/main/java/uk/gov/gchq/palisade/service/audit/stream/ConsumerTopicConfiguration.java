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

package uk.gov.gchq.palisade.service.audit.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.service.audit.common.Generated;
import uk.gov.gchq.palisade.service.audit.stream.ProducerTopicConfiguration.Topic;

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

    /**
     * Returns the discovery method
     *
     * @return the discovery method
     */
    @Generated
    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    /**
     * Sets the new discovery method
     *
     * @param discoveryMethod the new discovery method to set
     * @throws IllegalArgumentException if {@code discoveryMethod} is null
     */
    @Generated
    public void setDiscoveryMethod(final String discoveryMethod) {
        this.discoveryMethod = Optional.ofNullable(discoveryMethod)
                .orElseThrow(() -> new IllegalArgumentException("discoveryMethod cannot be null"));
    }

    /**
     * Returns the service name
     *
     * @return the service name
     */
    @Generated
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the new service name
     *
     * @param serviceName the new service name to set
     * @throws IllegalArgumentException if {@code serviceName} is null
     */
    @Generated
    public void setServiceName(final String serviceName) {
        this.serviceName = Optional.ofNullable(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("serviceName cannot be null"));
    }

    /**
     * Returns the use dispatcher
     *
     * @return the use dispatcher
     */
    @Generated
    public String getUseDispatcher() {
        return useDispatcher;
    }

    /**
     * Sets the use dispatcher
     *
     * @param useDispatcher the new use dispatcher to set
     * @throws IllegalArgumentException if {@code useDispatcher} is null
     */
    @Generated
    public void setUseDispatcher(final String useDispatcher) {
        this.useDispatcher = Optional.ofNullable(useDispatcher)
                .orElseThrow(() -> new IllegalArgumentException("useDispatcher cannot be null"));
    }

    /**
     * Returns the Kafka clients
     *
     * @return the Kafka clients
     */
    @Generated
    public Map<String, String> getKafkaClients() {
        return kafkaClients;
    }

    /**
     * Sets the new Kafka clients
     *
     * @param kafkaClients the new Kafka clients to set
     */
    @Generated
    public void setKafkaClients(final Map<String, String> kafkaClients) {
        this.kafkaClients = Optional.ofNullable(kafkaClients)
                .orElseThrow(() -> new IllegalArgumentException("kafkaClients cannot be null"));
    }

    /**
     * Returns the connection checker
     *
     * @return the connection checker
     */
    @Generated
    public Map<String, String> getConnectionChecker() {
        return connectionChecker;
    }

    /**
     * Sets the new connection checker
     *
     * @param connectionChecker the new connection checker to set
     */
    @Generated
    public void setConnectionChecker(final Map<String, String> connectionChecker) {
        this.connectionChecker = Optional.ofNullable(connectionChecker)
                .orElseThrow(() -> new IllegalArgumentException("connectionChecker cannot be null"));
    }

    /**
     * Returns the topics
     *
     * @return the topics
     */
    @Generated
    public Map<String, Topic> getTopics() {
        return topics;
    }

    /**
     * Sets the new topics
     *
     * @param topics the new topics to set
     */
    @Generated
    public void setTopics(final Map<String, Topic> topics) {
        this.topics = Optional.ofNullable(topics)
                .orElseThrow(() -> new IllegalArgumentException("topics cannot be null"));
    }

    /**
     * Gets the topic using the parameter name value
     *
     * @param topic the name of the topic
     * @return the {@link Topic} from the configuration
     */
    public Topic getTopic(final String topic) {
        return topics.get(topic);
    }
}
