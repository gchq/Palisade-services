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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for Akka Kafka producer
 */
@ConfigurationProperties("akka.kafka.producer")
public class ProducerTopicConfiguration {

    private String discoveryMethod;
    private String serviceName;
    private Integer parallelism;
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
     * Returns the parallelism
     *
     * @return the parallelism
     */
    @Generated
    public Integer getParallelism() {
        return parallelism;
    }

    /**
     * Sets the new parallelism
     *
     * @param parallelism the new parallelism to set
     * @throws IllegalArgumentException if {@code parallelism} is null
     */
    @Generated
    public void setParallelism(final Integer parallelism) {
        this.parallelism = Optional.ofNullable(parallelism)
                .filter(x -> x > 0)
                .orElseThrow(() -> new IllegalArgumentException("parallelism must be positive non-null"));
    }

    /**
     * Returns topics
     *
     * @return topics
     */
    @Generated
    public Map<String, Topic> getTopics() {
        return topics;
    }

    /**
     * Sets the new topics
     *
     * @param topics the new topics to set
     * @throws IllegalArgumentException if {@code topics} is null
     */
    @Generated
    public void setTopics(final Map<String, Topic> topics) {
        this.topics = Optional.ofNullable(topics)
                .orElseThrow(() -> new IllegalArgumentException("topics cannot be null"));
    }

    /**
     * Configuration for Kafka topics, comprised of topic name and partitions
     * Note that a service should not be aware of topic replication factor
     */
    public static class Topic {

        private String name;
        private Integer partitions;
        private Integer assignment;

        /**
         * Returns the topic name
         *
         * @return the topic name
         */
        @Generated
        public String getName() {
            return name;
        }

        /**
         * Sets the new topic name
         *
         * @param name the new topic name to set
         * @throws IllegalArgumentException if {@code name} is null
         */
        @Generated
        public void setName(final String name) {
            this.name = Optional.ofNullable(name)
                    .orElseThrow(() -> new IllegalArgumentException("name cannot be null"));
        }

        /**
         * Returns the number of partitions
         *
         * @return the number of partitions
         */
        @Generated
        public Integer getPartitions() {
            return partitions;
        }

        /**
         * Sets the new number of partitions
         *
         * @param partitions the new number of partitions to set
         * @throws IllegalArgumentException if {@code partitions} is null
         */
        @Generated
        public void setPartitions(final Integer partitions) {
            this.partitions = Optional.ofNullable(partitions)
                    .orElseThrow(() -> new IllegalArgumentException("partitions cannot be null"));
        }

        /**
         * Returns the assignment
         *
         * @return the assignment
         */
        @Generated
        public Integer getAssignment() {
            return assignment;
        }

        /**
         * Sets the new assignment
         *
         * @param assignment the new number of assignment to set
         * @throws IllegalArgumentException if {@code assignment} is null
         */
        @Generated
        public void setAssignment(final Integer assignment) {
            this.assignment = Optional.ofNullable(assignment)
                    .orElseThrow(() -> new IllegalArgumentException("assignment cannot be null"));
        }

    }

}
