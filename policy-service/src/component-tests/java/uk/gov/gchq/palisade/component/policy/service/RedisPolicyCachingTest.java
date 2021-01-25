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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import uk.gov.gchq.palisade.component.policy.service.RedisPolicyCachingTest.RedisInitializer;
import uk.gov.gchq.palisade.contract.policy.common.PolicyTestCommon;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.policy.service.PolicyServiceCachingProxy;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.redis.timeToLive=1s"}
)
@EnableCaching
@ContextConfiguration(initializers = {RedisInitializer.class})
@Import(RedisAutoConfiguration.class)
@ActiveProfiles({"redis"})
class RedisPolicyCachingTest extends PolicyTestCommon {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPolicyCachingTest.class);

    @Autowired
    private PolicyServiceCachingProxy cacheProxy;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Before the tests run, add policies to resources in the Policy service
     */
    @BeforeEach
    void setup() {
        // Add the system resource to the policy service
        cacheProxy.setResourceRules(TXT_SYSTEM.getId(), TXT_POLICY);

        // Add the directory resources to the policy service
        cacheProxy.setResourceRules(JSON_DIRECTORY.getId(), JSON_POLICY);
        cacheProxy.setResourceRules(SECRET_DIRECTORY.getId(), SECRET_POLICY);

        // Add the file resources to the policy service
        for (FileResource fileResource : FILE_RESOURCES) {
            cacheProxy.setResourceRules(fileResource.getId(), PASS_THROUGH_POLICY);
        }
    }

    protected void cleanCache() {
        requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        cleanCache();
    }

    @Test
    void testContextLoads() {
        assertThat(cacheProxy).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testAddedRuleIsRetrievable() {
        // Given - resources have been added as above
        // Given there is no underlying policy storage (gets must be wholly cache-based)

        for (Resource resource : FILE_RESOURCES) {
            // When
            Optional<Rules<LeafResource>> resourceRules = cacheProxy.getResourceRules(resource.getId());

            // Then
            assertThat(resourceRules)
                    .isPresent()
                    .get().isNotNull();
        }
    }

    @Test
    void testNonExistentRuleRetrieveFails() {
        // Given - the requested resource is not added

        // When
        Optional<Rules<Serializable>> recordRules = cacheProxy.getRecordRules("does not exist");

        // Then
        assertThat(recordRules).isEmpty();
    }

    @Test
    void testUpdateRules() {
        // Given I add a policy and resource
        final SystemResource systemResource = new SystemResource().id("/txt");
        final Rules<LeafResource> policy = new Rules<LeafResource>()
                .addRule("Resource serialised format is txt", new IsTextResourceRule());
        cacheProxy.setResourceRules(systemResource.getId(), policy);

        //Then I update the Policies resourceLevelRules
        final Rules<LeafResource> newPolicy = new Rules<LeafResource>()
                .addRule("NewSerialisedFormat", new IsTextResourceRule());
        cacheProxy.setResourceRules(systemResource.getId(), newPolicy);

        // When
        Optional<Rules<LeafResource>> recordRules = cacheProxy.getResourceRules(systemResource.getId());

        // Then the returned policy should have the updated resource rules
        assertThat(recordRules).isPresent();
        assertThat(recordRules.get().getRules()).usingRecursiveComparison().isEqualTo(newPolicy.getRules());
    }

    @Test
    void testCacheTtl() throws InterruptedException {
        // Given - the requested resource has policies available
        assertThat(cacheProxy.getResourceRules((ACCESSIBLE_JSON_TXT_FILE.getId()))).get().isNotNull();

        // Given - a sufficient amount of time has passed
        TimeUnit.SECONDS.sleep(1);

        // When - an old entry is requested
        Optional<Rules<LeafResource>> recordRules = cacheProxy.getResourceRules(ACCESSIBLE_JSON_TXT_FILE.getId());

        // Then - it has been evicted
        assertThat(recordRules).isEmpty();
    }


    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final int REDIS_PORT = 6379;

        static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withNetwork(Network.SHARED)
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
            RedisPolicyCachingTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }
}
