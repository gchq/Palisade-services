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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.contract.resource.kafka.KafkaTestConfiguration;
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
import uk.gov.gchq.palisade.util.AbstractResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceTest.class, ResourceApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false", "spring.data.redis.repositories.key-prefix=test:"}
)
@Import({KafkaTestConfiguration.class})
@ContextConfiguration(initializers = {RedisInitialiser.class})
@ActiveProfiles({"redis", "akka-test", "testcontainers"})
class RedisPersistenceTest {

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

    @TempDir
    static Path tempDir;

    // Temp files
    static File tempEmployeeAvroFile;
    static File tempEmployeeJsonFile;
    static File tempClientAvroFile;

    // Test resources
    static DirectoryResource testDirectory;
    static FileResource employeeAvroFile;
    static FileResource employeeJsonFile;
    static FileResource clientAvroFile;

    // Test requests
    static ResourceRequest testDirectoryRequest;
    static ResourceRequest employeeAvroRequest;

    private static final SimpleConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("data-service-mock");
    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final User USER = new User().userId("test-user");
    private static final String EMPLOYEE_TYPE = "employee";
    private static final String CLIENT_TYPE = "client";
    private static final String AVRO_FORMAT = "avro";
    private static final String JSON_FORMAT = "json";

    @BeforeAll
    static void startup() throws IOException {
        // Create temporary test files
        tempEmployeeAvroFile = Files.createFile(tempDir.resolve("employee.avro")).toFile();
        tempEmployeeJsonFile = Files.createFile(tempDir.resolve("employee.json")).toFile();
        tempClientAvroFile = Files.createFile(tempDir.resolve("client.avro")).toFile();

        // Create the test resources from the temp files
        testDirectory = (DirectoryResource) AbstractResourceBuilder.create("file:" + tempDir + "/");
        employeeAvroFile = ((FileResource) AbstractResourceBuilder.create("file:" + tempEmployeeAvroFile.getPath()))
                .type(EMPLOYEE_TYPE)
                .serialisedFormat(AVRO_FORMAT)
                .connectionDetail(DETAIL);
        employeeJsonFile = ((FileResource) AbstractResourceBuilder.create("file:" + tempEmployeeJsonFile.getPath()))
                .type(EMPLOYEE_TYPE)
                .serialisedFormat(JSON_FORMAT)
                .connectionDetail(DETAIL);
        clientAvroFile = ((FileResource) AbstractResourceBuilder.create("file:" + tempClientAvroFile.getPath()))
                .type(CLIENT_TYPE)
                .serialisedFormat(AVRO_FORMAT)
                .connectionDetail(DETAIL);

        // Create test requests
        testDirectoryRequest = ResourceRequest.Builder.create()
                .withUserId(USER.getUserId().getId())
                .withResourceId(testDirectory.getId())
                .withContext(CONTEXT)
                .withUser(USER);
        employeeAvroRequest = ResourceRequest.Builder.create()
                .withUserId(USER.getUserId().getId())
                .withResourceId(employeeAvroFile.getId())
                .withContext(CONTEXT)
                .withUser(USER);
    }

    @BeforeEach
    void setup() {

        // Wipe all keys from Redis
        redisTemplate.execute(conn -> conn.keyCommands()
                .keys(ByteBuffer.wrap("test:*".getBytes()))
                .flux()
                .flatMap(keys -> Flux.fromIterable(keys)
                        .flatMap(keyBb -> conn.keyCommands().del(keyBb))))
                .collectList().block();

        // Pre-populate
        for (LeafResource file : Arrays.asList(employeeJsonFile, employeeAvroFile, clientAvroFile)) {
            Source.single(file)
                    .via(persistenceLayer.withPersistenceById(testDirectory.getId()))
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
        List<AuditableResourceResponse> resourceAuditable = service.getResourcesByResource(testDirectoryRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        resourceAuditable.forEach(response -> resourceResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when getting a Resource by its directory, the correct resources are returned")
                .containsOnly(employeeJsonFile, employeeAvroFile, clientAvroFile);

        // When making a get request to the resource service by resource for a specific file
        resourceAuditable = service.getResourcesByResource(employeeAvroRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        resourceAuditable.forEach(response -> resourceResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when we get a Resource by itself, the correct resource is returned")
                .contains(employeeAvroFile);
    }

    @Test
    void testGetResourceById() {
        // Given - setup
        List<LeafResource> idResult = new LinkedList<>();

        // When making a get request to the resource service by resourceId for a directory
        List<AuditableResourceResponse> idAuditable = service.getResourcesById(testDirectoryRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        idAuditable.forEach(response -> idResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we get resources by the Id of the repository, the correct resources are returned")
                .containsOnly(employeeJsonFile, employeeAvroFile, clientAvroFile);
        idResult.clear();

        // When making a get request to the resource service by resourceId for a specific file
        idAuditable = service.getResourcesById(employeeAvroRequest)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        idAuditable.forEach(response -> idResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we request one resource by its ID, only the correct resource is returned")
                .containsOnly(employeeAvroFile);
    }

    @Test
    void testGetResourceByType() {
        // Given - setup
        List<LeafResource> typeResult = new LinkedList<>();

        // When making a get request to the resource service by type
        List<AuditableResourceResponse> typeAuditable = service.getResourcesByType(testDirectoryRequest, EMPLOYEE_TYPE)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        typeAuditable.forEach(response -> typeResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resources are returned")
                .containsOnly(employeeJsonFile, employeeAvroFile);
        typeResult.clear();

        // When making a get request to the resource service by type
        typeAuditable = service.getResourcesByType(testDirectoryRequest, CLIENT_TYPE)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        typeAuditable.forEach(response -> typeResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resource is returned")
                .containsOnly(clientAvroFile);
    }

    @Test
    void testGetResourceBySerialisedFormat() {
        // Given - setup
        List<LeafResource> formatResult = new LinkedList<>();

        // When making a get request to the resource service by serialisedFormat
        List<AuditableResourceResponse> formatAuditable = service.getResourcesBySerialisedFormat(testDirectoryRequest, AVRO_FORMAT)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        formatAuditable.forEach(response -> formatResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request resource by their format and directory, the correct resources are returned")
                .contains(clientAvroFile, employeeAvroFile);
        formatResult.clear();

        // When making a get request to the resource service by serialisedFormat
        formatAuditable = service.getResourcesBySerialisedFormat(testDirectoryRequest, JSON_FORMAT)
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        formatAuditable.forEach(response -> formatResult.add(response.getResourceResponse().resource));

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request a Resource by its format and directory, the correct resource is returned")
                .containsOnly(employeeJsonFile);
    }

}
