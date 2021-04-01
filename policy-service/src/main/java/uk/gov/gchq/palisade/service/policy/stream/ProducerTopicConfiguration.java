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

package uk.gov.gchq.palisade.service.policy.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.service.policy.common.Generated;

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
    public Integer getParallelism() {
        return parallelism;
    }

    @Generated
    public void setParallelism(final Integer parallelism) {
        this.parallelism = Optional.ofNullable(parallelism)
                .filter(x -> x > 0)
                .orElseThrow(() -> new IllegalArgumentException("parallelism must be positive non-null"));
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

    /**
     * Configuration for Kafka topics, comprised of topic name and partitions
     * Note that a service should not be aware of topic replication factor
     */
    public static class Topic {
        private String name;
        private Integer partitions;
        private Integer assignment;

        @Generated
        public String getName() {
            return name;
        }

        @Generated
        public void setName(final String name) {
            this.name = Optional.ofNullable(name)
                    .orElseThrow(() -> new IllegalArgumentException("name cannot be null"));
        }

        @Generated
        public Integer getPartitions() {
            return partitions;
        }

        @Generated
        public void setPartitions(final Integer partitions) {
            this.partitions = Optional.ofNullable(partitions)
                    .orElseThrow(() -> new IllegalArgumentException("partitions cannot be null"));
        }

        @Generated
        public Integer getAssignment() {
            return assignment;
        }

        @Generated
        public void setAssignment(final Integer assignment) {
            this.assignment = Optional.ofNullable(assignment)
                    .orElseThrow(() -> new IllegalArgumentException("assignment cannot be null"));
        }
    }

}
