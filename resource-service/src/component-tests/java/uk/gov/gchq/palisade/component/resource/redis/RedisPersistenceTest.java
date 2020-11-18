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

package uk.gov.gchq.palisade.component.resource.redis;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(
        classes = {ApplicationConfiguration.class},
        initializers = {RedisPersistenceTest.RedisInitializer.class}
)
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"redis"})
class RedisPersistenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPersistenceTest.class);

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
        assertThat(result1List).containsAll(expected);

        // When making a get request to the resource service by resource for a specific file
        resourcesByResource = FunctionalIterator.fromIterator(service.getResourcesByResource(EMPLOYEE_AVRO_FILE));
        resourcesByResource.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(result2List).containsAll(expected);
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
        assertThat(result1List).containsAll(expected);

        // When making a get request to the resource service by resourceId for a specific file
        resourcesById = FunctionalIterator.fromIterator(service.getResourcesById(EMPLOYEE_AVRO_FILE.getId()));
        resourcesById.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(result2List).containsAll(expected);
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
        assertThat(result1List).containsAll(expected);

        // When making a get request to the resource service by type
        resourcesByType = FunctionalIterator.fromIterator(service.getResourcesByType(CLIENT_TYPE));
        resourcesByType.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(CLIENT_AVRO_FILE);
        assertThat(result2List).containsAll(expected);

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
        assertThat(result1List).containsAll(expected);

        // When making a get request to the resource service by serialisedFormat
        resourcesBySerialisedFormat = FunctionalIterator.fromIterator(service.getResourcesBySerialisedFormat(JSON_FORMAT));
        resourcesBySerialisedFormat.forEachRemaining(result2List::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_JSON_FILE);
        assertThat(result2List).containsAll(expected);
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
            RedisPersistenceTest.LOGGER.info("Starting Redis with {}", redisContainerPort);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }
}
