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

package uk.gov.gchq.palisade.component.resource.repository.h2;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.R2dbcConfiguration;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;
import uk.gov.gchq.palisade.user.User;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DataR2dbcTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, R2dbcConfiguration.class, AkkaSystemConfig.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableR2dbcRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"dbtest"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class H2ComponentTest {

    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private ResourceServicePersistenceProxy proxy;
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
        testDirectory = (DirectoryResource) ResourceBuilder.create("file:" + tempDir + "/");
        employeeAvroFile = ((FileResource) ResourceBuilder.create("file:" + tempEmployeeAvroFile.getPath()))
                .type(EMPLOYEE_TYPE)
                .serialisedFormat(AVRO_FORMAT)
                .connectionDetail(DETAIL);
        employeeJsonFile = ((FileResource) ResourceBuilder.create("file:" + tempEmployeeJsonFile.getPath()))
                .type(EMPLOYEE_TYPE)
                .serialisedFormat(JSON_FORMAT)
                .connectionDetail(DETAIL);
        clientAvroFile = ((FileResource) ResourceBuilder.create("file:" + tempClientAvroFile.getPath()))
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
        for (FileResource file : Arrays.asList(employeeJsonFile, employeeAvroFile, clientAvroFile)) {
            Source.single(file)
                    .via(persistenceLayer.withPersistenceById(testDirectory.getId()))
                    .via(persistenceLayer.withPersistenceByType(file.getType()))
                    .via(persistenceLayer.withPersistenceBySerialisedFormat(file.getSerialisedFormat()))
                    .runWith(Sink.ignore(), materializer)
                    .toCompletableFuture().join();
        }
    }

    @Test
    void testGetResourceByResource() {
        // Given - setup

        // When making a get request to the resource service by resource for a directory
        List<LeafResource> resourceResult = proxy.getResourcesByResource(testDirectoryRequest)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        auditableResponse.getAuditErrorMessage().getError().printStackTrace();
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when getting a Resource by its directory, the correct resources are returned")
                .contains(employeeJsonFile, employeeAvroFile, clientAvroFile);

        // When making a get request to the resource service by resource for a specific file
        resourceResult = proxy.getResourcesByResource(employeeAvroRequest)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        auditableResponse.getAuditErrorMessage().getError().printStackTrace();
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(resourceResult)
                .as("Check that when we get a Resource by itself, the correct resource is returned")
                .containsOnly(employeeAvroFile);
    }

    @Test
    void testGetResourceById() {
        // Given - setup

        // When making a get request to the resource service by resourceId for a directory
        List<LeafResource> idResult = proxy.getResourcesById(testDirectoryRequest)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we get resources by the Id of the directory, the correct resources are returned")
                .contains(employeeJsonFile, employeeAvroFile, clientAvroFile);

        // When making a get request to the resource service by resourceId for a specific file
        idResult = proxy.getResourcesById(employeeAvroRequest)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(idResult)
                .as("Check that when we request one resource by its ID, only the correct resource is returned")
                .containsOnly(employeeAvroFile);
    }

    @Test
    void testGetResourceByType() {
        // Given - setup

        // When making a get request to the resource service by type
        List<LeafResource> typeResult = proxy.getResourcesByType(testDirectoryRequest, EMPLOYEE_TYPE)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resources are returned")
                .containsExactly(employeeJsonFile, employeeAvroFile);

        // When making a get request to the resource service by type
        typeResult = proxy.getResourcesByType(testDirectoryRequest, CLIENT_TYPE)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(typeResult)
                .as("Check that when we request a resource by the directory and type, the correct resource is returned")
                .containsOnly(clientAvroFile);
    }

    @Test
    void testGetResourceBySerialisedFormat() {
        // Given - setup

        // When making a get request to the resource service by serialisedFormat
        List<LeafResource> formatResult = proxy.getResourcesBySerialisedFormat(testDirectoryRequest, AVRO_FORMAT)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request resource by their format and directory, the correct resources are returned")
                .containsExactly(employeeAvroFile, clientAvroFile);

        // When making a get request to the resource service by serialisedFormat
        formatResult = proxy.getResourcesBySerialisedFormat(testDirectoryRequest, JSON_FORMAT)
                .map(auditableResponse -> {
                    if (auditableResponse.getAuditErrorMessage() != null) {
                        fail("Expecting success but error was thrown: %s", auditableResponse.getAuditErrorMessage());
                    }
                    return auditableResponse.getResourceResponse().resource;
                })
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();

        // Then assert that the expected resource(s) are returned
        assertThat(formatResult)
                .as("Check that when we request a Resource by its format and directory, the correct resource is returned")
                .containsOnly(employeeJsonFile);
    }
}
