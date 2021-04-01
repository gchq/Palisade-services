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
package uk.gov.gchq.palisade.component.user.service;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import uk.gov.gchq.palisade.component.user.service.RedisUserCachingTest.RedisInitializer;
import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserServiceCachingProxy;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.redis.timeToLive=1s"}
)
@EnableCaching
@ContextConfiguration(initializers = {RedisInitializer.class})
@Import(RedisAutoConfiguration.class)
@ActiveProfiles({"redis"})
class RedisUserCachingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUserCachingTest.class);

    @Autowired
    private UserServiceCachingProxy cacheProxy;

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
        assertThat(cacheProxy)
                .as("Check that the UserService has loaded the caching layer")
                .isNotNull();

        assertThat(redisTemplate)
                .as("Check that Redis is ready")
                .isNotNull();
    }

    @Test
    void testAddedUserIsRetrievable() {
        // Given we create a new User and UserRequest
        var user = new User().userId("added-user")
                .addAuths(Collections.singleton("authorisation"))
                .addRoles(Collections.singleton("role"));

        // When we add the user to the cache
        cacheProxy.addUser(user);
        var addedUser = cacheProxy.getUser(user.getUserId().getId());

        // Then
        assertThat(addedUser)
                .as("Check that the retrieved User is the same as the User we created")
                .usingRecursiveComparison()
                .isEqualTo(user);
    }

    @Test
    void testNonExistentUserRetrieveFails() {
        // Given the user is not added to the cache

        // When we get the user from the cache
        var noSuchUserId = assertThrows(NoSuchUserIdException.class,
                () -> cacheProxy.getUser("definitely-not-a-real-user"), "testNonExistentUser should throw noSuchIdException");

        // Then - it is no longer found, it has been evicted
        assertThat(noSuchUserId)
                .as("Check that the correct message is added to the Exception")
                .extracting("Message")
                .isEqualTo("No userId matching definitely-not-a-real-user found in cache");
    }

    @Test
    void testUpdateUser() {
        // Given we create an original user, and then update the users auths and roles
        User originalUser = new User().userId("updatable-user")
                .addAuths(Collections.singleton("auth"))
                .addRoles(Collections.singleton("role"));

        User updatedUser = new User().userId(originalUser.getUserId())
                .addAuths(Collections.singleton("newAuth"))
                .addRoles(Collections.singleton("newRole"));

        // When we add the original User
        cacheProxy.addUser(originalUser);
        // Then update the same User
        cacheProxy.addUser(updatedUser);

        // When we get the updated user
        User returnedUser = cacheProxy.getUser(updatedUser.getUserId().getId());

        // Then the User has been updated
        assertThat(returnedUser)
                .as("Check that the original User has been updated")
                .usingRecursiveComparison()
                .isEqualTo(updatedUser);
    }

    @Test
    void testTtl() throws InterruptedException {
        // Given - a user was added a long time ago (ttl set to 1s in application.yaml)
        User user = new User().userId("ttl-test-user")
                .addAuths(Collections.singleton("authorisation"))
                .addRoles(Collections.singleton("role"));

        UserRequest request = UserRequest.Builder.create()
                .withUserId(user.getUserId().getId())
                .withResourceId("test/resource")
                .withContext(new Context().purpose("purpose"));

        // When we add the user to the cache
        cacheProxy.addUser(user);

        // Then sleep for longer than the ttl duration
        TimeUnit.SECONDS.sleep(1);

        // When - we try to access stale cache data
        var noSuchUserId = assertThrows(NoSuchUserIdException.class,
                () -> cacheProxy.getUser(request.userId), "testTTL should throw noSuchIdException");

        // Then - it is no longer found, it has been evicted
        assertThat(noSuchUserId)
                .as("Check that the correct message is added to the Exception")
                .extracting("Message")
                .isEqualTo("No userId matching ttl-test-user found in cache");
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
            RedisUserCachingTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }
}
