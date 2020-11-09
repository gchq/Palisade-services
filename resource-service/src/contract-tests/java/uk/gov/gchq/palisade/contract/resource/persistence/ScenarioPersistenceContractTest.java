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

package uk.gov.gchq.palisade.contract.resource.persistence;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.ResourceApplication;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ResourceApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles({"dbtest", "akka"})
class ScenarioPersistenceContractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioPersistenceContractTest.class);

    @Autowired
    private JpaPersistenceLayer persistenceLayer;

    @Autowired
    private StreamingResourceServiceProxy client;

    private static final ConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("http://localhost:8082");


    private static final String ROOT_PATH =  System.getProperty("user.dir") + "/src/contract-tests/resources/root/";

    static {
        new File(ROOT_PATH + "empty-dir/").mkdir();
    }

    /**
     * Scenario as follows, where (F)iles, (D)irectories and (S)ystems are annotated respectively and numbered in query order
     *
     *       5 -> S
     *           / \
     *     3 -> D   D <- 4
     *         / \
     *   2 -> D   D
     *       / \   \
     * 1 -> F   F   F
     */

    private static final DirectoryResource ROOT_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH).toURI());

    private static final DirectoryResource TOP_LEVEL_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/").toURI());
    private static final DirectoryResource EMPTY_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "empty-dir/").toURI());

    private static final DirectoryResource MULTI_FILE_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/").toURI());
    private static final DirectoryResource SINGLE_FILE_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/single-file-dir/").toURI());

    private static final FileResource MULTI_FILE_ONE = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/multiFileOne.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(DETAIL);
    private static final FileResource MULTI_FILE_TWO = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/multiFileTwo.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(DETAIL);

    private static final FileResource SINGLE_FILE = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/single-file-dir/singleFile.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(DETAIL);

    // We want to test the StreamingResourceServiceProxy class, but consuming using a Supplier<OutputStream> is complicated
    // Instead, send everything through the REST interface
    // Test is still marked as transactional as we poke and prod the persistence layer directly to see what is persisted and what isn't
    // For spring reasons, we can't just mark the extractResourceCompleteness method as transactional
    @Test
    @Transactional(readOnly = true)
    void runThroughTestScenario() {
        FunctionalIterator<LeafResource> returned;
        FunctionalIterator<Resource> persisted;
        List<LeafResource> returnedList = new ArrayList<>();
        List<LeafResource> expectedReturned;
        List<Resource> persistedList = new ArrayList<>();
        List<Resource> expectedPersisted;

        // When - Pt 1
        LOGGER.debug("Getting resources for {}", MULTI_FILE_ONE.getId());
        returned = client.getResourcesByResource(MULTI_FILE_ONE);
        returned.forEachRemaining(returnedList::add);
        expectedReturned = Collections.singletonList(MULTI_FILE_ONE);
        expectedPersisted = Collections.singletonList(MULTI_FILE_ONE);
        persisted = FunctionalIterator.fromIterator(expectedPersisted.iterator());
        persisted = persistenceLayer.withPersistenceById(MULTI_FILE_ONE.getId(), persisted);
        persisted.forEachRemaining(persistedList::add);
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returned.peek(resource -> LOGGER.debug("Returned: {}", resource.getId()));
        assertThat(returnedList.size()).isEqualTo(expectedReturned.size());
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persistedList.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persistedList.size()).isEqualTo(expectedPersisted.size());
        LOGGER.debug("");
        returnedList.clear();
        persistedList.clear();
        LOGGER.debug("");


        // When - Pt 2
        LOGGER.debug("Getting resources for {}", MULTI_FILE_DIR.getId());
        returned = client.getResourcesByResource(MULTI_FILE_DIR);
        returned.forEachRemaining(returnedList::add);
        expectedReturned = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO);
        expectedPersisted = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR);
        persisted = FunctionalIterator.fromIterator(expectedPersisted.iterator());
        persisted = persistenceLayer.withPersistenceById(MULTI_FILE_DIR.getId(), persisted);
        persisted.forEachRemaining(persistedList::add);
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returned.peek(resource -> LOGGER.debug("Returned: {}", resource.getId()));
        assertThat(returnedList.size()).isEqualTo(expectedReturned.size());
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persistedList.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persistedList.size()).isEqualTo(expectedPersisted.size());
        LOGGER.debug("");
        returnedList.clear();
        persistedList.clear();
        LOGGER.debug("");


        // When - Pt 3
        LOGGER.debug("Getting resources for {}", TOP_LEVEL_DIR.getId());
        returned = client.getResourcesByResource(TOP_LEVEL_DIR);
        returned.forEachRemaining(returnedList::add);
        expectedReturned = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, SINGLE_FILE);
        expectedPersisted = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR, SINGLE_FILE, SINGLE_FILE_DIR, TOP_LEVEL_DIR);
        persisted = FunctionalIterator.fromIterator(expectedPersisted.iterator());
        persisted = persistenceLayer.withPersistenceById(TOP_LEVEL_DIR.getId(), persisted);
        persisted.forEachRemaining(persistedList::add);
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returned.peek(resource -> LOGGER.debug("Returned: {}", resource.getId()));
        assertThat(returnedList.size()).isEqualTo(expectedReturned.size());
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persistedList.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persistedList.size()).isEqualTo(expectedPersisted.size());
        LOGGER.debug("");
        returnedList.clear();
        persistedList.clear();
        LOGGER.debug("");


        // When - Pt 4
        LOGGER.debug("Getting resources for {}", EMPTY_DIR.getId());
        returned = client.getResourcesByResource(EMPTY_DIR);
        returned.forEachRemaining(returnedList::add);
        expectedPersisted = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR, SINGLE_FILE, SINGLE_FILE_DIR, TOP_LEVEL_DIR, EMPTY_DIR);
        persisted = FunctionalIterator.fromIterator(expectedPersisted.iterator());
        persisted = persistenceLayer.withPersistenceById(EMPTY_DIR.getId(), persisted);
        persisted.forEachRemaining(persistedList::add);
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        returned.peek(resource -> LOGGER.debug("Returned: {}", resource.getId()));
        assertThat(returnedList.size()).isZero();
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persistedList.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persistedList.size()).isEqualTo(expectedPersisted.size());
        LOGGER.debug("");
        returnedList.clear();
        persistedList.clear();
        LOGGER.debug("");


        // When - Pt 5
        LOGGER.debug("Getting resources for {}", ROOT_DIR.getId());
        returned = client.getResourcesByResource(ROOT_DIR);
        returned.forEachRemaining(returnedList::add);
        expectedReturned = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, SINGLE_FILE);
        expectedPersisted = Arrays.asList(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR, SINGLE_FILE, SINGLE_FILE_DIR, TOP_LEVEL_DIR, EMPTY_DIR, ROOT_DIR);
        persisted = FunctionalIterator.fromIterator(expectedPersisted.iterator());
        persisted = persistenceLayer.withPersistenceById(ROOT_DIR.getId(), persisted);
        persisted.forEachRemaining(persistedList::add);
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returned.peek(resource -> LOGGER.debug("Returned: {}", resource.getId()));
        assertThat(returnedList.size()).isEqualTo(expectedReturned.size());
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persistedList.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persistedList.size()).isEqualTo(expectedPersisted.size());
        LOGGER.debug("");
        returnedList.clear();
        persistedList.clear();
        LOGGER.debug("");
    }
}
