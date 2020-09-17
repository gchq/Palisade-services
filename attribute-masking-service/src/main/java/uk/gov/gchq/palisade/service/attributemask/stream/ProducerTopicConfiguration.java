package uk.gov.gchq.palisade.service.attributemask.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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

    public static class Topic {
        private String name;
        private Integer partitions;

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
    }

}
