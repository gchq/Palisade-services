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

package uk.gov.gchq.palisade.contract.attributemask.redis;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.attributemask.ContractTestData;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.stream.PropertiesConfigurer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceContractTest.class, AttributeMaskingApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"spring.data.redis.repositories.timeToLive.AuthorisedRequestEntity=1s", "akka.discovery.config.services.kafka.from-config=false"}
)
@Import({RedisPersistenceContractTest.KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {RedisPersistenceContractTest.KafkaInitializer.class, RedisPersistenceContractTest.RedisInitializer.class})
@ActiveProfiles({"redis", "akka-test"})
class RedisPersistenceContractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPersistenceContractTest.class);

    @Autowired
    private AttributeMaskingService service;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    protected void cleanCache() {
        requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        cleanCache();
    }

    @Test
    void testContextLoads() {
        assertThat(service)
                .as("Check that the service has been autowired successfully")
                .isNotNull();

        assertThat(redisTemplate)
                .as("Check that the redisTemplate has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testAuthorisedRequestsAreStoredInRedis() {
        // Given we have some request data
        var token = ContractTestData.REQUEST_TOKEN;
        var request = ContractTestData.REQUEST_OBJ;

        // When a request is made to store the request for a given token
        service.storeAuthorisedRequest(token, request).join();

        // Then the request is persisted in redis
        var redisKey = "AuthorisedRequestEntity:" + new AuthorisedRequestEntity.AuthorisedRequestEntityId(token, request.getResourceId()).getUniqueId();

        assertThat(redisTemplate.keys(redisKey))
                .as("Check that the key is stored in the redisTemplate")
                .hasSize(1);

        // Values for the entity are correct
        var redisHash = redisTemplate.boundHashOps(redisKey).entries();
        assertThat(redisHash)
                .as("Check that the returned hash contains the correct token and resourceId")
                .containsEntry("token", ContractTestData.REQUEST_TOKEN)
                .containsEntry("resourceId", ContractTestData.REQUEST_OBJ.getResource().getId());
    }

    @Test
    void testAuthorisedRequestsAreEvictedAfterTtlExpires() throws InterruptedException {
        // Given we have some request data
        var token = ContractTestData.REQUEST_TOKEN;
        var request = ContractTestData.REQUEST_OBJ;

        // When a request is made to store the request for a given token
        service.storeAuthorisedRequest(token, request).join();
        TimeUnit.SECONDS.sleep(2);
        // Then the offset is persisted in redis
        var redisHash = redisTemplate.keys("AuthorisedRequestEntity:" + new AuthorisedRequestEntity.AuthorisedRequestEntityId(token, request.getResourceId()).getUniqueId());
        assertThat(redisHash)
                .as("Check that there is no key stored in the redisTemplate showing that the resource has expired")
                .isEmpty();
    }

    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final int REDIS_PORT = 6379;

        static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NotNull final ConfigurableApplicationContext context) {
            context.getEnvironment().setActiveProfiles("redis", "akka-test");
            // Start container
            redis.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + redis.getContainerIpAddress();
            // Configure the test container random port
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
            configurableApplicationContext.getEnvironment().setActiveProfiles("akka-test", "redis");
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
                    new NewTopic("rule", 1, (short) 1),
                    new NewTopic("masked-resource", 1, (short) 1),
                    new NewTopic("error", 1, (short) 1));

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
