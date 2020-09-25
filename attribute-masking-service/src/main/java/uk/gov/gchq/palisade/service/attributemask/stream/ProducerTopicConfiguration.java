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

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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
    public Integer getParallelism() {
        return parallelism;
    }

    @Generated
    public void setParallelism(final Integer parallelism) {
        requireNonNull(parallelism);
        this.parallelism = parallelism;
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
            requireNonNull(name);
            this.name = name;
        }

        @Generated
        public Integer getPartitions() {
            return partitions;
        }

        @Generated
        public void setPartitions(final Integer partitions) {
            requireNonNull(partitions);
            this.partitions = partitions;
        }

        @Generated
        public Integer getAssignment() {
            return assignment;
        }

        @Generated
        public void setAssignment(final Integer assignment) {
            requireNonNull(assignment);
            this.assignment = assignment;
        }
    }

}
