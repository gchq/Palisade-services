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

package uk.gov.gchq.palisade.contract.filteredresource.kafka;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.filteredresource.stream.PropertiesConfigurer;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.5.1")
            .withReuse(true);
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaInitializer.class);
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();

    static void createTopics(final List<NewTopic> newTopics) throws ExecutionException, InterruptedException {
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", KafkaInitializer.KAFKA_CONTAINER.getFirstMappedPort())))) {
            admin.createTopics(newTopics);
            LOGGER.info("created topics: " + admin.listTopics().names().get());
        }
    }

    @Override
    public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
        configurableApplicationContext.getEnvironment().setActiveProfiles("k8s", "akka", "debug");
        KAFKA_CONTAINER.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        KAFKA_CONTAINER.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        KAFKA_CONTAINER.addEnv("KAFKA_ADVERTISED_HOST_NAME", "zookeeper");
        KAFKA_CONTAINER.addEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181");
        KAFKA_CONTAINER.start();

        // test kafka config
        String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
        String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port=" + KAFKA_CONTAINER.getFirstMappedPort();
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
    }

    @Configuration
    public static class Config {

        private final List<NewTopic> topics = List.of(
                new NewTopic("masked-resource", 1, (short) 1),
                new NewTopic("masked-resource-offset", 1, (short) 1),
                new NewTopic("success", 1, (short) 1),
                new NewTopic("error", 1, (short) 1));

        @Bean
        @ConditionalOnMissingBean
        static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
            return new PropertiesConfigurer(resourceLoader, environment);
        }

        @Bean
        KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
            createTopics(this.topics);
            return KAFKA_CONTAINER;
        }

        @Bean
        @Primary
        ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
            LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
            return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                    props.getAllActiveProperties().entrySet().stream()
                            .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                    Stream.of(new SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
        }

        @Bean
        @Primary
        Materializer materializer(final ActorSystem system) {
            return Materializer.createMaterializer(system);
        }
    }

    static class ErrorDeserializer implements Deserializer<AuditErrorMessage> {
        @Override
        public AuditErrorMessage deserialize(final String s, final byte[] errorResponse) {
            try {
                return MAPPER.readValue(errorResponse, AuditErrorMessage.class);
            } catch (IOException e) {
                throw new SerializationFailedException("Failed to deserialize " + new String(errorResponse), e);
            }
        }
    }

}