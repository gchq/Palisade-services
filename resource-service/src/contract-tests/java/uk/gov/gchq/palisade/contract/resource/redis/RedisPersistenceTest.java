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

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.contract.resource.kafka.KafkaInitializer;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.ResourceApplication;
import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.user.User;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceTest.class, ResourceApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import({KafkaInitializer.Config.class})
@ContextConfiguration(initializers = {RedisInitializer.class, KafkaInitializer.class})
@ActiveProfiles({"redis", "akka-test"})
class RedisPersistenceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPersistenceTest.class);

    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private ResourceServicePersistenceProxy service;
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;
    @Autowired
    private Materializer materializer;

    /**
     * Scenario as follows, where (F)iles, (D)irectories and (S)ystems are annotated respectively
     * <pre>
     *    S
     *    |
     *    D
     *  / | \
     * F  F  F
     * </pre>
     */

    private static final SimpleConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("data-service-mock");
    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final User USER = new User().userId("test-user");
    private static final String EMPLOYEE_TYPE = "employee";
    private static final String CLIENT_TYPE = "client";
    private static final String AVRO_FORMAT = "avro";
    private static final String JSON_FORMAT = "json";
    private static final DirectoryResource TEST_DIRECTORY = (DirectoryResource) ResourceBuilder.create("file:/test/");
    private static final FileResource EMPLOYEE_AVRO_FILE = ((FileResource) ResourceBuilder.create("file:/test/employee.avro"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);
    private static final FileResource EMPLOYEE_JSON_FILE = ((FileResource) ResourceBuilder.create("file:/test/employee.json"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(JSON_FORMAT)
            .connectionDetail(DETAIL);
    private static final FileResource CLIENT_AVRO_FILE = ((FileResource) ResourceBuilder.create("file:/test/client.avro"))
            .type(CLIENT_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);

    public static final ResourceRequest TEST_DIRECTORY_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(TEST_DIRECTORY.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest EMPLOYEE_AVRO_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(EMPLOYEE_AVRO_FILE.getId())
            .withContext(CONTEXT)
            .withUser(USER);

    @BeforeEach
    void setup() {
        // Wipe all keys from Redis
        redisTemplate.execute(conn -> conn.keyCommands()
                .keys(ByteBuffer.wrap("*".getBytes()))
                .flux()
                .flatMap(keys -> Flux.fromIterable(keys)
                        .flatMap(keyBb -> conn.keyCommands().del(keyBb))))
                .collectList().block();

        // Pre-populate
        for (LeafResource file : Arrays.asList(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE)) {
            Source.single(file)
                    .via(persistenceLayer.withPersistenceById(TEST_DIRECTORY.getId()))
                    .via(persistenceLayer.withPersistenceByType(file.getType()))
                    .via(persistenceLayer.withPersistenceBySerialisedFormat(file.getSerialisedFormat()))
                    .runWith(Sink.seq(), materializer)
                    .toCompletableFuture().join();
        }
    }

    @Test
    void testGetResourceByResource() {
        // Given - setup
        List<LeafResource> resourceResult = new LinkedList<>();

        // When making a get request to the resource service by resource for a directory
        List<AuditableResourceResponse> resourceAuditable = service.getResourcesByResource(TEST_DIRECTORY_REQUEST)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        resourceAuditable.forEach(response -> resourceResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when getting a Resource by its directory, the correct resources are returned")
                .containsOnly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);

        // When making a get request to the resource service by resource for a specific file
        resourceAuditable = service.getResourcesByResource(EMPLOYEE_AVRO_REQUEST)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        resourceAuditable.forEach(response -> resourceResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when we get a Resource by itself, the correct resource is returned")
                .contains(EMPLOYEE_AVRO_FILE);
    }

    @Test
    void testGetResourceById() {
        // Given - setup
        List<LeafResource> idResult = new LinkedList<>();

        // When making a get request to the resource service by resourceId for a directory
        List<AuditableResourceResponse> idAuditable = service.getResourcesById(TEST_DIRECTORY_REQUEST)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        idAuditable.forEach(response -> idResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we get resources by the Id of the repository, the correct resources are returned")
                .containsOnly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);
        idResult.clear();

        // When making a get request to the resource service by resourceId for a specific file
        idAuditable = service.getResourcesById(EMPLOYEE_AVRO_REQUEST)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        idAuditable.forEach(response -> idResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we request one resource by its ID, only the correct resource is returned")
                .containsOnly(EMPLOYEE_AVRO_FILE);
    }

    @Test
    void testGetResourceByType() {
        // Given - setup
        List<LeafResource> typeResult = new LinkedList<>();

        // When making a get request to the resource service by type
        List<AuditableResourceResponse> typeAuditable = service.getResourcesByType(TEST_DIRECTORY_REQUEST, EMPLOYEE_TYPE)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        typeAuditable.forEach(response -> typeResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resources are returned")
                .containsOnly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE);
        typeResult.clear();

        // When making a get request to the resource service by type
        typeAuditable = service.getResourcesByType(TEST_DIRECTORY_REQUEST, CLIENT_TYPE)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        typeAuditable.forEach(response -> typeResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resource is returned")
                .containsOnly(CLIENT_AVRO_FILE);
    }

    @Test
    void testGetResourceBySerialisedFormat() {
        // Given - setup
        List<LeafResource> formatResult = new LinkedList<>();

        // When making a get request to the resource service by serialisedFormat
        List<AuditableResourceResponse> formatAuditable = service.getResourcesBySerialisedFormat(TEST_DIRECTORY_REQUEST, AVRO_FORMAT)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        formatAuditable.forEach(response -> formatResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request resource by their format and directory, the correct resources are returned")
                .contains(CLIENT_AVRO_FILE, EMPLOYEE_AVRO_FILE);
        formatResult.clear();

        // When making a get request to the resource service by serialisedFormat
        formatAuditable = service.getResourcesBySerialisedFormat(TEST_DIRECTORY_REQUEST, JSON_FORMAT)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        formatAuditable.forEach(response -> formatResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request a Resource by its format and directory, the correct resource is returned")
                .containsOnly(EMPLOYEE_JSON_FILE);
    }

}