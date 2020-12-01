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

package uk.gov.gchq.palisade.contract.filteredresource.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;

import uk.gov.gchq.palisade.contract.filteredresource.ContractTestData;
import uk.gov.gchq.palisade.contract.filteredresource.redis.RedisPersistenceContractTest.Initializer;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceContractTest.class, FilteredResourceApplication.class},
        properties = {"spring.data.redis.repositories.timeToLive.TokenOffsetEntity=1s"}
)
@ActiveProfiles("redis")
@ContextConfiguration(initializers = Initializer.class)
class RedisPersistenceContractTest {

    private static final int REDIS_PORT = 6379;

    protected void cleanCache() {
        requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NonNull final ConfigurableApplicationContext context) {
            // Start container
            redis.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + redis.getContainerIpAddress();
            // Configure the testcontainer random port
            String redisContainerPort = "spring.redis.port=" + redis.getMappedPort(REDIS_PORT);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }

    @AfterEach
    void tearDown() {
        cleanCache();
    }

    @Autowired
    private OffsetEventService service;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testContextLoads() {
        assertThat(service).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testTopicOffsetsAreStoredInRedis() {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.queuePointer).join();

        // Then the offset is persisted in redis
        final String redisKey = "TokenOffsetEntity:" + token;
        assertThat(redisTemplate.keys(redisKey)).hasSize(1);

        // Values for the entity are correct
        final Map<Object, Object> redisHash = redisTemplate.boundHashOps(redisKey).entries();
        assertThat(redisHash)
                .containsEntry("token", ContractTestData.REQUEST_TOKEN)
                .containsEntry("offset", ContractTestData.TOPIC_OFFSET_MESSAGE.queuePointer.toString());
    }

    @Test
    void testTopicOffsetsAreEvictedAfterTtlExpires() throws InterruptedException {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.queuePointer).join();
        TimeUnit.SECONDS.sleep(1);

        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys("TokenOffsetEntity:" + token)).isEmpty();
    }

}
