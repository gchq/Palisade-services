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

package uk.gov.gchq.palisade.contract.resource.redis;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.ResourceApplication;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.service.resource.stream.PropertiesConfigurer;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceContractTest.class, ResourceApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"spring.data.redis.repositories.timeToLive.AuthorisedRequestEntity=1s", "akka.discovery.config.services.kafka.from-config=false"}
)
@Import({RedisPersistenceContractTest.KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {RedisPersistenceContractTest.KafkaInitializer.class, RedisPersistenceContractTest.RedisInitializer.class})
@ActiveProfiles({"redis", "akkatest"})
class RedisPersistenceContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPersistenceContractTest.class);

    @Autowired
    private JpaPersistenceLayer persistenceLayer;

    @Autowired
    private StreamingResourceServiceProxy service;

    /**
     * Scenario as follows, where (F)iles, (D)irectories and (S)ystems are annotated respectively
     *    S
     *    |
     *    D
     *  / | \
     * F  F  F
     */

    private static final SimpleConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("data-service-mock");
    private static final String EMPLOYEE_TYPE = "employee";
    private static final String CLIENT_TYPE = "client";
    private static final String AVRO_FORMAT = "avro";
    private static final String JSON_FORMAT = "json";
    private static final SystemResource SYSTEM_ROOT = (SystemResource) ResourceBuilder.create("file:/");
    private static final DirectoryResource TEST_DIRECTORY = (DirectoryResource) ResourceBuilder.create("file:/test/");
    private static final LeafResource EMPLOYEE_AVRO_FILE = ((LeafResource) ResourceBuilder.create("file:/test/employee.avro"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);
    private static final LeafResource EMPLOYEE_JSON_FILE = ((LeafResource) ResourceBuilder.create("file:/test/employee.json"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(JSON_FORMAT)
            .connectionDetail(DETAIL);
    private static final LeafResource CLIENT_AVRO_FILE = ((LeafResource) ResourceBuilder.create("file:/test/client.avro"))
            .type(CLIENT_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);

    @BeforeEach
    @Transactional
    void setup() {
        for (LeafResource file : Arrays.asList(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE)) {
            FunctionalIterator<LeafResource> fileIterator = FunctionalIterator
                    .fromIterator(Collections.singletonList(file).iterator());
            fileIterator = persistenceLayer.withPersistenceById(TEST_DIRECTORY.getId(), fileIterator);
            fileIterator = persistenceLayer.withPersistenceByType(file.getType(), fileIterator);
            fileIterator = persistenceLayer.withPersistenceBySerialisedFormat(file.getSerialisedFormat(), fileIterator);
            while (fileIterator.hasNext()) {
                fileIterator.next();
            }
        }
    }

    @Test
    void getTestResourceByResource() {
        // Given - setup
        List<LeafResource> result1List = new ArrayList<>();
        List<LeafResource> result2List = new ArrayList<>();

        // When making a get request to the resource service by resource for a directory
        FunctionalIterator<LeafResource> resourcesByResource = FunctionalIterator.fromIterator(service.getResourcesByResource(TEST_DIRECTORY));
        resourcesByResource.forEachRemaining(result1List::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE, CLIENT_AVRO_FILE);
        assertThat(result1List.size()).isEqualTo(expected.size());

        // When making a get request to the resource service by resource for a specific file
        resourcesByResource = FunctionalIterator.fromIterator(service.getResourcesByResource(EMPLOYEE_AVRO_FILE));
        resourcesByResource.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(result2List.size()).isEqualTo(expected.size());
    }

    @Test
    void getTestResourceById() {
        // Given - setup
        List<LeafResource> result1List = new ArrayList<>();
        List<LeafResource> result2List = new ArrayList<>();

        // When making a get request to the resource service by resourceId for a directory
        FunctionalIterator<LeafResource> resourcesById = FunctionalIterator.fromIterator(service.getResourcesById(TEST_DIRECTORY.getId()));
        resourcesById.forEachRemaining(result1List::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE, CLIENT_AVRO_FILE);
        assertThat(result1List.size()).isEqualTo(expected.size());

        // When making a get request to the resource service by resourceId for a specific file
        resourcesById = FunctionalIterator.fromIterator(service.getResourcesById(EMPLOYEE_AVRO_FILE.getId()));
        resourcesById.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(result2List.size()).isEqualTo(expected.size());
    }

    @Test
    void getTestResourceByType() {
        // Given - setup
        List<LeafResource> result1List = new ArrayList<>();
        List<LeafResource> result2List = new ArrayList<>();

        // When making a get request to the resource service by type
        FunctionalIterator<LeafResource> resourcesByType = FunctionalIterator.fromIterator(service.getResourcesByType(EMPLOYEE_TYPE));
        resourcesByType.forEachRemaining(result1List::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE);
        assertThat(result1List.size()).isEqualTo(expected.size());

        // When making a get request to the resource service by type
        resourcesByType = FunctionalIterator.fromIterator(service.getResourcesByType(CLIENT_TYPE));
        resourcesByType.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(CLIENT_AVRO_FILE);
        assertThat(result2List.size()).isEqualTo(expected.size());

    }

    @Test
    void getTestResourceBySerialisedFormat() {
        // Given - setup
        List<LeafResource> result1List = new ArrayList<>();
        List<LeafResource> result2List = new ArrayList<>();

        // When making a get request to the resource service by serialisedFormat
        FunctionalIterator<LeafResource> resourcesBySerialisedFormat = FunctionalIterator.fromIterator(service.getResourcesBySerialisedFormat(AVRO_FORMAT));
        resourcesBySerialisedFormat.forEachRemaining(result1List::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);
        assertThat(result1List.size()).isEqualTo(expected.size());

        // When making a get request to the resource service by serialisedFormat
        resourcesBySerialisedFormat = FunctionalIterator.fromIterator(service.getResourcesBySerialisedFormat(JSON_FORMAT));
        resourcesBySerialisedFormat.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_JSON_FILE);
        assertThat(result2List.size()).isEqualTo(expected.size());
    }

    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final int REDIS_PORT = 6379;

        static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NotNull final ConfigurableApplicationContext context) {
            context.getEnvironment().setActiveProfiles("redis", "akkatest");
            // Start container
            redis.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + redis.getContainerIpAddress();
            // Configure the testcontainer random port
            String redisContainerPort = "spring.redis.port=" + redis.getMappedPort(REDIS_PORT);
            RedisPersistenceContractTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }

    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static KafkaContainer kafka = new KafkaContainer("5.5.1")
                .withReuse(true);

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.getEnvironment().setActiveProfiles("akkatest", "redis");
            kafka.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
            kafka.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
            kafka.start();

            // test kafka config
            String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
            String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port" + kafka.getFirstMappedPort();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
        }

        static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka) throws ExecutionException, InterruptedException {
            try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", kafka.getFirstMappedPort())))) {
                admin.createTopics(newTopics);
                LOGGER.info("created topics: " + admin.listTopics().names().get());
            }
        }

        @Configuration
        public static class Config {

            private final List<NewTopic> topics = List.of(
                    new NewTopic("user", 3, (short) 1),
                    new NewTopic("resource", 3, (short) 1),
                    new NewTopic("error", 3, (short) 1));

            @Bean
            KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
                createTopics(this.topics, kafka);
                return kafka;
            }

            @Bean
            @ConditionalOnMissingBean
            static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
                return new PropertiesConfigurer(resourceLoader, environment);
            }

            @Bean
            @Primary
            ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
                RedisPersistenceContractTest.LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
                return ActorSystem.create("actor-with-overrides", props.toHoconConfig(Stream.concat(
                        props.getAllActiveProperties().entrySet().stream()
                                .filter(kafkaPort -> !kafkaPort.getKey().equals("akka.discovery.config.services.kafka.endpoints[0].port")),
                        Stream.of(new AbstractMap.SimpleEntry<>("akka.discovery.config.services.kafka.endpoints[0].port", Integer.toString(kafka.getFirstMappedPort()))))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
            }

            @Bean
            @Primary
            Materializer materializer(final ActorSystem system) {
                return Materializer.createMaterializer(system);
            }
        }
    }
}
