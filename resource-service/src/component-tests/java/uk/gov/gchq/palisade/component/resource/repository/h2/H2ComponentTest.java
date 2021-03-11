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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.R2dbcConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.AVRO_FORMAT;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.CLIENT_AVRO_FILE;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.CLIENT_TYPE;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.EMPLOYEE_AVRO_FILE;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.EMPLOYEE_AVRO_REQUEST;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.EMPLOYEE_JSON_FILE;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.EMPLOYEE_TYPE;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.JSON_FORMAT;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.TEST_DIRECTORY;
import static uk.gov.gchq.palisade.component.resource.CommonTestData.TEST_DIRECTORY_REQUEST;

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

    @BeforeEach
    void setup() {
        for (FileResource file : Arrays.asList(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE)) {
            Source.single(file)
                    .via(persistenceLayer.withPersistenceById(TEST_DIRECTORY.getId()))
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
        List<LeafResource> resourceResult = proxy.getResourcesByResource(TEST_DIRECTORY_REQUEST)
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
                .containsExactly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);

        // When making a get request to the resource service by resource for a specific file
        resourceResult = proxy.getResourcesByResource(EMPLOYEE_AVRO_REQUEST)
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
                .containsOnly(EMPLOYEE_AVRO_FILE);
    }

    @Test
    void testGetResourceById() {
        // Given - setup

        // When making a get request to the resource service by resourceId for a directory
        List<LeafResource> idResult = proxy.getResourcesById(TEST_DIRECTORY_REQUEST)
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
                .as("Check that when we get resources by the Id of the repository, the correct resources are returned")
                .containsExactly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);

        // When making a get request to the resource service by resourceId for a specific file
        idResult = proxy.getResourcesById(EMPLOYEE_AVRO_REQUEST)
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
                .containsOnly(EMPLOYEE_AVRO_FILE);
    }

    @Test
    void testGetResourceByType() {
        // Given - setup

        // When making a get request to the resource service by type
        List<LeafResource> typeResult = proxy.getResourcesByType(TEST_DIRECTORY_REQUEST, EMPLOYEE_TYPE)
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
                .containsExactly(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE);

        // When making a get request to the resource service by type
        typeResult = proxy.getResourcesByType(TEST_DIRECTORY_REQUEST, CLIENT_TYPE)
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
                .containsOnly(CLIENT_AVRO_FILE);
    }

    @Test
    void testGetResourceBySerialisedFormat() {
        // Given - setup

        // When making a get request to the resource service by serialisedFormat
        List<LeafResource> formatResult = proxy.getResourcesBySerialisedFormat(TEST_DIRECTORY_REQUEST, AVRO_FORMAT)
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
                .containsExactly(EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);

        // When making a get request to the resource service by serialisedFormat
        formatResult = proxy.getResourcesBySerialisedFormat(TEST_DIRECTORY_REQUEST, JSON_FORMAT)
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
                .containsOnly(EMPLOYEE_JSON_FILE);
    }
}
