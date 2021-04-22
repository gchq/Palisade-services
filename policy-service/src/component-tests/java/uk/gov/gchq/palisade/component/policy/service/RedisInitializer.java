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

package uk.gov.gchq.palisade.component.policy.service;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int REDIS_PORT = 6379;

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(REDIS_PORT)
            .withNetwork(Network.SHARED)
            .withReuse(true);

    @Override
    public void initialize(@NonNull final ConfigurableApplicationContext context) {
        context.getEnvironment().setActiveProfiles("redis", "akkatest");
        // Start container
        REDIS.start();

        // Override Redis configuration
        String redisContainerIP = "spring.redis.host=" + REDIS.getContainerIpAddress();
        // Configure the testcontainer random port
        String redisContainerPort = "spring.redis.port=" + REDIS.getMappedPort(REDIS_PORT);
        RedisPolicyCachingTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
        // Override the configuration at runtime
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
    }
}
