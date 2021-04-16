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

package uk.gov.gchq.palisade.contract.topicoffset.kafka;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.service.topicoffset.stream.PropertiesConfigurer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

@Configuration
@ConditionalOnProperty(
        value = "akka.discovery.config.services.kafka.from-config",
        havingValue = "false"
)
public class KafkaTestConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTestConfiguration.class);

    private final List<NewTopic> topics = List.of(
            new NewTopic("masked-resource", 3, (short) 1),
            new NewTopic("masked-resource-offset", 3, (short) 1),
            new NewTopic("error", 3, (short) 1));

    @Bean
    @ConditionalOnMissingBean
    static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
        return new PropertiesConfigurer(resourceLoader, environment);
    }

    @Bean
    KafkaContainer kafkaContainer() throws Exception {
        final KafkaContainer container = new KafkaContainer("5.5.1");
        container.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        container.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        container.addEnv("KAFKA_ADVERTISED_HOST_NAME", "zookeeper");
        container.addEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181");
        container.start();

        createTopics(this.topics, container);

        return container;
    }

    @Bean
    @Primary
    Materializer getMaterializer(final ActorSystem system) {
        return Materializer.createMaterializer(system);
    }

    @Bean
    @Primary
    ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
        return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                props.getAllActiveProperties().entrySet().stream()
                        .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                Stream.of(new AbstractMap.SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka) throws ExecutionException, InterruptedException {
        try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, getKafkaBrokers(kafka)))) {
            admin.createTopics(newTopics);
            LOGGER.info("created topics: " + admin.listTopics().names().get());
        }
    }

    static String getKafkaBrokers(final KafkaContainer kafka) {
        Integer mappedPort = kafka.getFirstMappedPort();
        String brokers = String.format("%s:%d", "localhost", mappedPort);
        LOGGER.info("brokers: " + brokers);
        return brokers;
    }
}
