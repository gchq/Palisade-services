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
package uk.gov.gchq.palisade.component.policy.service;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
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
import org.testcontainers.containers.KafkaContainer;

import uk.gov.gchq.palisade.contract.policy.PolicyTestCommon;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.StubResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.PolicyApplication;
import uk.gov.gchq.palisade.service.policy.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;
import uk.gov.gchq.palisade.service.policy.stream.PropertiesConfigurer;
import uk.gov.gchq.palisade.service.request.Policy;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"caffeine", "akkatest"})
@Import({CaffeinePolicyCachingTest.KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {CaffeinePolicyCachingTest.KafkaInitializer.class})
@SpringBootTest(
        classes = {PolicyApplication.class, ApplicationConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.caffeine.spec=expireAfterWrite=1s, maximumSize=100"}
)
class CaffeinePolicyCachingTest extends PolicyTestCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeinePolicyCachingTest.class);

    @Autowired
    private PolicyServiceCachingProxy policyService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Cleans up the caches used by the Policy Service
     */
    private void forceCleanUp() {
        List<String> caches = Arrays.asList("resourcePolicy", "typePolicy", "accessPolicy");
        caches.forEach(x -> ((Cache<?, ?>) Objects.requireNonNull(cacheManager.getCache(x)).getNativeCache()).cleanUp());
    }

    /**
     * Before the tests run, add policies to resources in the Policy service
     */
    @BeforeEach
    void setup() {
        // Add the system resource to the policy service
       policyService.setResourcePolicy(TXT_SYSTEM, TXT_POLICY);

        // Add the directory resources to the policy service
       policyService.setResourcePolicy(JSON_DIRECTORY, JSON_POLICY);
       policyService.setResourcePolicy(SECRET_DIRECTORY, SECRET_POLICY);

        // Add the file resources to the policy service
        for (FileResource fileResource : FILE_RESOURCES) {
            policyService.setResourcePolicy(fileResource, PASS_THROUGH_POLICY);
        }
    }

    /**
     * Tests that the service loads
     */
    @Test
    void testContextLoads() {
        assertThat(policyService).isNotNull();
    }

    /**
     * Tests that the correct policy is retrieved for the resource
     */
    @Test
    void testAddedPolicyIsRetrievable() {
        // Given - resources have been added as above
        // Given there is no underlying policy storage (gets must be wholly cache-based)

        for (Resource resource : FILE_RESOURCES) {
            // When
            Optional<Policy> policy = policyService.getPolicy(resource);

            // Then
            assertThat(policy).isPresent();
        }
    }

    /**
     * Tests that if the resource is not added for the policy then nothing is returned
     */
    @Test
    void testNonExistentPolicyRetrieveFails() {
        // Given - the requested resource is not added

        // When
        Optional<Policy> policy = policyService.getPolicy(new FileResource().id("does not exist").type("null").serialisedFormat("null").parent(new SystemResource().id("also does not exist")));

        // Then
        assertThat(policy).isEmpty();
    }

    /**
     * Tests that if the cache is full, the first entry is removed
     */
    @Test
    void testCacheMaxSize() {
        /// Given - the cache is overfilled
        Function<Integer, Resource> makeResource = i -> new StubResource(i.toString(), i.toString(), i.toString(), new SimpleConnectionDetail().serviceName(i.toString()));
        Function<Integer, Policy> makePolicy = i -> new Policy<>().resourceLevelRule(i.toString(), new PassThroughRule<>());
        for (int count = 0; count <= 100; ++count) {
            policyService.setResourcePolicy(makeResource.apply(count), makePolicy.apply(count));
        }

        // When - we try to get the first (now-evicted) entry
        forceCleanUp();
        Optional<Policy> cachedPolicy = policyService.getPolicy(makeResource.apply(0));

        // Then - it has been evicted
        assertThat(cachedPolicy).isEmpty();
    }

    /**
     * Tests that if the entry ttl expires it is removed
     * @throws InterruptedException in case TimeUnit.sleep throws an exception
     */
    @Test
    void testCacheTtl() throws InterruptedException {
        // Given - the requested resource has policies available
        assertThat(policyService.getPolicy(ACCESSIBLE_JSON_TXT_FILE)).isPresent();
        // Given - a sufficient amount of time has passed

        TimeUnit.SECONDS.sleep(1);
        forceCleanUp();

        // When - an old entry is requested
        Optional<Policy> cachedPolicy = policyService.getPolicy(ACCESSIBLE_JSON_TXT_FILE);

        // Then - it has been evicted
        assertThat(cachedPolicy).isEmpty();
    }

    /**
     * Kafka configuration
     */
    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static KafkaContainer kafka = new KafkaContainer("5.5.1")
                .withReuse(true);

        static void createTopics(final List<NewTopic> newTopics, final KafkaContainer kafka) throws ExecutionException, InterruptedException {
            try (AdminClient admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%d", "localhost", kafka.getFirstMappedPort())))) {
                admin.createTopics(newTopics);
                LOGGER.info("created topics: " + admin.listTopics().names().get());
            }
        }

        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.getEnvironment().setActiveProfiles("akkatest", "caffeine");
            kafka.addEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
            kafka.addEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
            kafka.start();

            // test kafka config
            String kafkaConfig = "akka.discovery.config.services.kafka.from-config=false";
            String kafkaPort = "akka.discovery.config.services.kafka.endpoints[0].port" + kafka.getFirstMappedPort();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, kafkaConfig, kafkaPort);
        }

        @Configuration
        public static class Config {

            private final List<NewTopic> topics = List.of(
                    new NewTopic("resource", 3, (short) 1),
                    new NewTopic("rule", 3, (short) 1),
                    new NewTopic("error", 3, (short) 1));

            @Bean
            @ConditionalOnMissingBean
            static PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
                return new PropertiesConfigurer(resourceLoader, environment);
            }

            @Bean
            KafkaContainer kafkaContainer() throws ExecutionException, InterruptedException {
                createTopics(this.topics, kafka);
                return kafka;
            }

            @Bean
            @Primary
            ActorSystem actorSystem(final PropertiesConfigurer props, final KafkaContainer kafka, final ConfigurableApplicationContext context) {
                CaffeinePolicyCachingTest.LOGGER.info("Starting Kafka with port {}", kafka.getFirstMappedPort());
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
