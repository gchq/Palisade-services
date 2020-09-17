package uk.gov.gchq.palisade.service.attributemask.stream;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("akka.kafka.consumer")
public class ConsumerTopicConfiguration {
    private String discoveryMethod;
    private String serviceName;
    private String useDispatcher;
    private Map<String, String> kafkaClients = new HashMap<>();
    private Map<String, String> connectionChecker = new HashMap<>();
    private Map<String, ProducerTopicConfiguration.Topic> topics = new HashMap<>();

    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    public void setDiscoveryMethod(String discoveryMethod) {
        this.discoveryMethod = discoveryMethod;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUseDispatcher() {
        return useDispatcher;
    }

    public void setUseDispatcher(String useDispatcher) {
        this.useDispatcher = useDispatcher;
    }

    public Map<String, String> getKafkaClients() {
        return kafkaClients;
    }

    public void setKafkaClients(Map<String, String> kafkaClients) {
        this.kafkaClients = kafkaClients;
    }

    public Map<String, String> getConnectionChecker() {
        return connectionChecker;
    }

    public void setConnectionChecker(Map<String, String> connectionChecker) {
        this.connectionChecker = connectionChecker;
    }

    public Map<String, ProducerTopicConfiguration.Topic> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, ProducerTopicConfiguration.Topic> topics) {
        this.topics = topics;
    }
}
