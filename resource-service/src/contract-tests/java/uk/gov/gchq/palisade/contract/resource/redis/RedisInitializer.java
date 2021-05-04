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

package uk.gov.gchq.palisade.contract.resource.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisInitializer.class);
    private static final int REDIS_PORT = 6379;

    @Override
    public void initialize(@NonNull final ConfigurableApplicationContext context) {
        final String fullImageName = context.getEnvironment().getRequiredProperty("testcontainers.redis.image");
        final String defaultImageName = context.getEnvironment().getRequiredProperty("testcontainers.redis.default.image");

        DockerImageName redisImageName;
        try {
            redisImageName = DockerImageName.parse(fullImageName)
                .asCompatibleSubstituteFor(defaultImageName);
            redisImageName.assertValid();
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Image name {} was invalid, falling back to default name {}", fullImageName, defaultImageName, ex);
            redisImageName = DockerImageName.parse(defaultImageName);
        }
        final GenericContainer<?> redis = new GenericContainer<>(redisImageName)
            .withExposedPorts(REDIS_PORT)
            .withReuse(true)
            .withStartupTimeout(Duration.ofMinutes(1))
            .withStartupAttempts(3);

        // Start container
        redis.start();

        // Override Redis configuration
        String redisContainerIP = "spring.redis.host=" + redis.getContainerIpAddress();
        // Configure the testcontainer random port
        String redisContainerPort = "spring.redis.port=" + redis.getMappedPort(REDIS_PORT);
        LOGGER.info("Starting Redis with {}", redisContainerPort);
        // Override the configuration at runtime
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
    }
}
