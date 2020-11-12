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

package uk.gov.gchq.palisade.contract.resource.h2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.ResourceApplication;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ResourceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles({"dbtest", "akkatest"})
class H2ContractTest {

    @Autowired
    private JpaPersistenceLayer persistenceLayer;

    @Autowired
    private StreamingResourceServiceProxy proxy;

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

    @BeforeEach
    @Transactional
    void setup() {
        for (FileResource file : Arrays.asList(EMPLOYEE_JSON_FILE, EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE)) {
            FunctionalIterator<LeafResource> fileIterator = FunctionalIterator
                    .fromIterator(Collections.singletonList((LeafResource) file).iterator());
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
        List<LeafResource> returnedList = new ArrayList<>();

        // When making a get request to the resource service by resource for a directory
        FunctionalIterator<LeafResource> resourcesByResource = proxy.getResourcesByResource(TEST_DIRECTORY);
        resourcesByResource.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE, CLIENT_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
        returnedList.clear();

        // When making a get request to the resource service by resource for a specific file
        resourcesByResource = proxy.getResourcesByResource(EMPLOYEE_AVRO_FILE);
        resourcesByResource.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
    }

    @Test
    void getTestResourceById() {
        // Given - setup
        List<LeafResource> returnedList = new ArrayList<>();

        // When making a get request to the resource service by resourceId for a directory
        Iterator<LeafResource> resourcesById = proxy.getResourcesById(TEST_DIRECTORY.getId());
        resourcesById.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE, CLIENT_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
        returnedList.clear();

        // When making a get request to the resource service by resourceId for a specific file
        resourcesById = proxy.getResourcesById(EMPLOYEE_AVRO_FILE.getId());
        resourcesById.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
    }

    @Test
    void getTestResourceByType() {
        // Given - setup
        List<LeafResource> returnedList = new ArrayList<>();

        // When making a get request to the resource service by type
        Iterator<LeafResource> resourcesByType = proxy.getResourcesByType(EMPLOYEE_TYPE);
        resourcesByType.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, EMPLOYEE_JSON_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
        returnedList.clear();

        // When making a get request to the resource service by type
        resourcesByType = proxy.getResourcesByType(CLIENT_TYPE);
        resourcesByType.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(CLIENT_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());

    }

    @Test
    void getTestResourceBySerialisedFormat() {
        // Given - setup
        List<LeafResource> returnedList = new ArrayList<>();

        // When making a get request to the resource service by serialisedFormat
        Iterator<LeafResource> resourcesBySerialisedFormat = proxy.getResourcesBySerialisedFormat(AVRO_FORMAT);
        resourcesBySerialisedFormat.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        List<LeafResource> expected = Arrays.asList(EMPLOYEE_AVRO_FILE, CLIENT_AVRO_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());
        returnedList.clear();

        // When making a get request to the resource service by serialisedFormat
        resourcesBySerialisedFormat = proxy.getResourcesBySerialisedFormat(JSON_FORMAT);
        resourcesBySerialisedFormat.forEachRemaining(returnedList::add);

        // Then assert that the expected resource(s) are returned
        expected = Collections.singletonList(EMPLOYEE_JSON_FILE);
        assertThat(returnedList.size()).isEqualTo(expected.size());

    }
}
