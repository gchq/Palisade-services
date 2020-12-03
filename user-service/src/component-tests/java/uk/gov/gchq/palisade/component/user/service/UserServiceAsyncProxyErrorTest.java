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

package uk.gov.gchq.palisade.component.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.component.user.service.UserServiceAsyncProxyErrorTest.RedisInitializer;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.AuditableUserResponse;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserServiceAsyncProxy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify the handling of exceptions, and the population of audit objects during stream processing
 */
@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.redis.timeToLive=1s"}
)
@EnableCaching
@ContextConfiguration(initializers = {RedisInitializer.class})
@Import(RedisAutoConfiguration.class)
@ActiveProfiles({"redis"})
class UserServiceAsyncProxyErrorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceAsyncProxyErrorTest.class);


    private static final Function<Integer, String> REQUEST_FACTORY_JSON = i ->
            String.format("{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"purpose\"}}}", i, i);

    @Autowired
    private UserServiceAsyncProxy userServiceAsyncProxy;
    @Autowired
    private ObjectMapper mapper;
    private final Function<Integer, JsonNode> requestFactoryNode = i -> {
        try {
            return this.mapper.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    private final Function<Integer, UserRequest> requestFactoryObj = i -> {
        try {
            return this.mapper.treeToValue(requestFactoryNode.apply(i), UserRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };

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
        assertThat(userServiceAsyncProxy).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testGetUserSuccess() {
        // Given a user request
        final UserRequest userRequest = requestFactoryObj.apply(1);
        final User user = new User().userId("test-user-id");
        // When adding to the cache
        this.userServiceAsyncProxy.addUser(user);

        // Then retrieving from the cache
        final CompletableFuture<AuditableUserResponse> subject = this.userServiceAsyncProxy.getUser(userRequest);

        // Check the CompletableFuture hasn't finished
        assertThat(subject.isDone()).isFalse();
        // Then complete the future
        AuditableUserResponse auditableUserResponse = subject.join();
        // Then Check it has been completed
        assertThat(subject.isDone()).isTrue();

        // Then the service suppresses exception and populates Audit object
        assertThat(auditableUserResponse.getAuditErrorMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isNull();

        // Then the cached User is the same as the original User
        assertThat(auditableUserResponse.getUserResponse().getUserId())
                .isEqualTo(user.getUserId().getId());
    }

    @Test
    void testGetUserFailure() {
        // When adding a user
        final User user = new User().userId("Not-a-real-user");
        this.userServiceAsyncProxy.addUser(user);

        // Then retrieving a different user
        final UserRequest userRequest = requestFactoryObj.apply(1);
        final CompletableFuture<AuditableUserResponse> subject = this.userServiceAsyncProxy.getUser(userRequest);

        // Check the CompletableFuture hasn't finished
        assertThat(subject.isDone()).isFalse();
        // Then complete the future
        AuditableUserResponse auditableUserResponse = subject.join();
        // Then Check it has been completed
        assertThat(subject.isDone()).isTrue();

        // Then check that there is an error message
        assertThat(auditableUserResponse.getAuditErrorMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isNotNull();

        // Then check the error message contains the correct message
        assertThat(auditableUserResponse.getAuditErrorMessage().getError().getMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isEqualTo(NoSuchUserIdException.class.getName() + ": No userId matching test-user-id found in cache");
    }

    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final int REDIS_PORT = 6379;

        static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withNetwork(Network.SHARED)
                .withReuse(true);

        @Override
        public void initialize(@NotNull final ConfigurableApplicationContext context) {
            context.getEnvironment().setActiveProfiles("redis");
            // Start container
            REDIS.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + REDIS.getContainerIpAddress();
            // Configure the test container random port
            String redisContainerPort = "spring.redis.port=" + REDIS.getMappedPort(REDIS_PORT);
            UserServiceAsyncProxyErrorTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }

}
