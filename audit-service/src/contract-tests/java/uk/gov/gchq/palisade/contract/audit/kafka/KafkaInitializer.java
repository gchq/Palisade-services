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

package uk.gov.gchq.palisade.contract.audit.kafka;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import uk.gov.gchq.palisade.service.audit.stream.PropertiesConfigurer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

/**
 * An application context initializer that
 */
public class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaInitializer.class);

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.5.1").withReuse(true);

    @Override
    public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {

        configurableApplicationContext.getEnvironment().setActiveProfiles("akka-test");

        KAFKA_CONTAINER.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        KAFKA_CONTAINER.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        KAFKA_CONTAINER.start();

        // test kafka config
        String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
        String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port" + KAFKA_CONTAINER.getFirstMappedPort();

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);

    }


    /**
     * Configuration providing the test beans to be inject into test classes that
     * require access to various objects to support access to Kafka/Akka
     */
    @Configuration
    public static class Config {


        @Bean
        KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
            var topics = List.of(
                new NewTopic("success", 1, (short) 1),
                new NewTopic("error", 1, (short) 1));
            createTopics(topics, KAFKA_CONTAINER);
            return KAFKA_CONTAINER;
        }

        @Bean
        @ConditionalOnMissingBean
        static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
            return new PropertiesConfigurer(resourceLoader, environment);
        }

        @Bean
        @Primary
        ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {

            LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
            var portKey = "akka.discovery.config.services.kafka.endpoints[0].port";
            var port = Integer.toString(kafka.getFirstMappedPort());

            // remove current port if found and then add back in with the port Kafka is
            // listening on
            var config = props.toHoconConfig(Stream
                .concat(
                    props.getAllActiveProperties()
                        .entrySet()
                        .stream()
                        .filter(kafkaPort -> !kafkaPort.getKey().equals(portKey)),
                    Stream.of(new AbstractMap.SimpleEntry<>(portKey, port)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

            return ActorSystem.create("actor-with-overrides", config);
        }

        @Bean
        @Primary
        Materializer materializer(final ActorSystem system) {
            return Materializer.createMaterializer(system);
        }

        @Bean
        Serializer<JsonNode> requestSerialiser(@Autowired final ObjectMapper objectMapper) {
            return (final String s, final JsonNode auditRequest) -> {
                try {
                    return objectMapper.writeValueAsBytes(auditRequest);
                } catch (JsonProcessingException e) {
                    throw new SerializationFailedException("Failed to serialise " + auditRequest.toString(), e);
                }
            };
        }

        @Bean
        Serializer<String> stringSerialiser() {
            return new StringSerializer();
        }

        private static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka)
            throws ExecutionException, InterruptedException {
            var host = String.format("%s:%d", "localhost", kafka.getFirstMappedPort());
            var adminProperties = Map.<String, Object>of(BOOTSTRAP_SERVERS_CONFIG, host);
            try (var admin = AdminClient.create(adminProperties)) {
                admin.createTopics(newTopics);
                LOGGER.info("created topics: " + admin.listTopics().names().get());
            }
        }

    }
}