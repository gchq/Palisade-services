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

package uk.gov.gchq.palisade.component.resource.repository;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.reader.common.ConnectionDetail;
import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.User;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.Resource;
import uk.gov.gchq.palisade.reader.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.reader.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.R2dbcConfiguration;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;
import uk.gov.gchq.palisade.service.resource.model.AuditableResourceResponse;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.service.resource.repository.CompletenessRepository;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.ResourceServicePersistenceProxy;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, R2dbcConfiguration.class, AkkaSystemConfig.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableR2dbcRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"db-test"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ScenarioPersistenceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioPersistenceTest.class);

    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private ResourceServicePersistenceProxy proxy;
    @Autowired
    private Materializer materializer;
    @Autowired
    private CompletenessRepository completenessRepository;

    private static final ConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("http://localhost:8082");
    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final User USER = new User().userId("test-user");

    private static final String ROOT_PATH = System.getProperty("user.dir") + "/src/contract-tests/resources/root/";

    static {
        new File(ROOT_PATH + "empty-dir/").mkdir();
    }

    /**
     * Scenario as follows, where (F)iles, (D)irectories and (S)ystems are annotated respectively and numbered in query order
     * <pre>
     *       5 -> S
     *           / \
     *     3 -> D   D <- 4
     *         / \
     *   2 -> D   D
     *       / \   \
     * 1 -> F   F   F
     * </pre>
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

    private static final ResourceRequest MULTI_FILE_ONE_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(MULTI_FILE_ONE.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    private static final ResourceRequest MULTI_FILE_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(MULTI_FILE_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    private static final ResourceRequest TOP_LEVEL_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(TOP_LEVEL_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    private static final ResourceRequest EMPTY_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(EMPTY_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    private static final ResourceRequest ROOT_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(ROOT_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);

    // We want to test the StreamingResourceServiceProxy class, but consuming using a Supplier<OutputStream> is complicated
    // Instead, send everything through the REST interface
    // Test is still marked as transactional as we poke and prod the persistence layer directly to see what is persisted and what isn't
    // For spring reasons, we can't just mark the extractResourceCompleteness method as transactional
    @Test
    void testRunThroughTestScenario() {
        // Given -
        // Variables used to store returned results from the resource-service
        Set<AuditableResourceResponse> returnedAuditable;
        Set<Resource> persisted;
        // Expected variables for persisted resource(s) and returned resource(s)
        Set<LeafResource> expectedReturned;
        Set<Resource> expectedPersisted;
        // Creation of returned sets for the specific requests that are made
        Set<LeafResource> returnedMultiFileRequest = new HashSet<>();
        Set<LeafResource> returnedMultiFileDirRequest = new HashSet<>();
        Set<LeafResource> returnedTopLevelDirRequest = new HashSet<>();
        Set<LeafResource> returnedEmptyDirRequest = new HashSet<>();
        Set<LeafResource> returnedRootDirRequest = new HashSet<>();

        // When - Pt 1: Specific resource requested from resource-service
        LOGGER.info("Part 1 of the Scenario Test: Get a Single File Resource");
        LOGGER.debug("Getting resources for {}", MULTI_FILE_ONE.getId());
        returnedAuditable = new HashSet<>(proxy.getResourcesByResource(MULTI_FILE_ONE_REQUEST)
                .runWith(Sink.seq(), materializer).toCompletableFuture().join());
        // Get the returned resource from the AuditableResourceResponse and add to specific returned set
        returnedAuditable.forEach(response -> returnedMultiFileRequest.add(response.getResourceResponse().resource));
        expectedReturned = Collections.singleton(MULTI_FILE_ONE);
        expectedPersisted = Collections.singleton(MULTI_FILE_ONE);
        persisted = expectedPersisted.stream().filter(this::extractResourceCompleteness).collect(Collectors.toSet());
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returnedAuditable.forEach(response -> LOGGER.debug("Returned: {}", response.getResourceResponse().resource.getId()));
        assertThat(returnedMultiFileRequest).isEqualTo(expectedReturned);
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persisted.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persisted).isEqualTo(expectedPersisted);
        LOGGER.debug("");
        LOGGER.info("");


        // When - Pt 2: Request contains the id of a directory containing multiple files
        LOGGER.info("Part 2 of the Scenario Test: Get a Directory containing Multiple Files");
        LOGGER.debug("Getting resources for {}", MULTI_FILE_DIR.getId());
        returnedAuditable = new HashSet<>(proxy.getResourcesByResource(MULTI_FILE_DIR_REQUEST)
                .runWith(Sink.seq(), materializer).toCompletableFuture().join());
        // Get the returned resource from the AuditableResourceResponse and add to specific returned set
        returnedAuditable.forEach(response -> returnedMultiFileDirRequest.add(response.getResourceResponse().resource));
        expectedReturned = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO);
        expectedPersisted = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR);
        persisted = expectedPersisted.stream().filter(this::extractResourceCompleteness).collect(Collectors.toSet());
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returnedAuditable.forEach(response -> LOGGER.debug("Returned: {}", response.getResourceResponse().resource.getId()));
        assertThat(returnedMultiFileDirRequest).isEqualTo(expectedReturned);
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persisted.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persisted).isEqualTo(expectedPersisted);
        LOGGER.debug("");
        LOGGER.info("");


        // When - Pt 3: Request contains the id of a directory containing multiple directories with file(s)
        LOGGER.info("Part 3 of the Scenario Test: Get a Directory containing Child Directories");
        LOGGER.debug("Getting resources for {}", TOP_LEVEL_DIR.getId());
        returnedAuditable = new HashSet<>(proxy.getResourcesByResource(TOP_LEVEL_DIR_REQUEST)
                .runWith(Sink.seq(), materializer).toCompletableFuture().join());
        // Get the returned resource from the AuditableResourceResponse and add to specific returned set
        returnedAuditable.forEach(response -> returnedTopLevelDirRequest.add(response.getResourceResponse().resource));
        expectedReturned = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, SINGLE_FILE);
        expectedPersisted = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR,
                SINGLE_FILE, SINGLE_FILE_DIR, TOP_LEVEL_DIR);
        persisted = expectedPersisted.stream().filter(this::extractResourceCompleteness).collect(Collectors.toSet());
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returnedAuditable.forEach(response -> LOGGER.debug("Returned: {}", response.getResourceResponse().resource.getId()));
        assertThat(returnedTopLevelDirRequest).isEqualTo(expectedReturned);
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persisted.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persisted).isEqualTo(expectedPersisted);
        LOGGER.debug("");
        LOGGER.info("");


        // When - Pt 4: Request contains the id of an empty directory (no sub-directories or files)
        LOGGER.info("Part 4 of the Scenario Test: Get a Directory that does not contain anything (empty)");
        LOGGER.debug("Getting resources for {}", EMPTY_DIR.getId());
        returnedAuditable = new HashSet<>(proxy.getResourcesByResource(EMPTY_DIR_REQUEST)
                .runWith(Sink.seq(), materializer).toCompletableFuture().join());
        // Get the returned resource from the AuditableResourceResponse and add to specific returned set
        returnedAuditable.forEach(response -> returnedEmptyDirRequest.add(response.getResourceResponse().resource));
        expectedReturned = Collections.emptySet();
        expectedPersisted = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR,
                SINGLE_FILE, SINGLE_FILE_DIR, TOP_LEVEL_DIR, EMPTY_DIR);
        persisted = expectedPersisted.stream().filter(this::extractResourceCompleteness).collect(Collectors.toSet());
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        LOGGER.debug("Expected: nothing");
        returnedAuditable.forEach(response -> LOGGER.debug("Returned: {}", response.getResourceResponse().resource.getId()));
        assertThat(returnedEmptyDirRequest).isEqualTo(expectedReturned);
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persisted.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persisted).isEqualTo(expectedPersisted);
        LOGGER.debug("");
        LOGGER.info("");


        // When - Pt 5: Request contains the id of the root directory
        LOGGER.info("Part 5 of the Scenario Test: Get a Top Level (root) Directory");
        LOGGER.debug("Getting resources for {}", ROOT_DIR.getId());
        returnedAuditable = new HashSet<>(proxy.getResourcesByResource(ROOT_DIR_REQUEST)
                .runWith(Sink.seq(), materializer).toCompletableFuture().join());
        // Get the returned resource from the AuditableResourceResponse and add to specific returned set
        returnedAuditable.forEach(response -> returnedRootDirRequest.add(response.getResourceResponse().resource));
        expectedReturned = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, SINGLE_FILE);
        expectedPersisted = Set.of(MULTI_FILE_ONE, MULTI_FILE_TWO, MULTI_FILE_DIR, SINGLE_FILE,
                SINGLE_FILE_DIR, TOP_LEVEL_DIR, EMPTY_DIR, ROOT_DIR);
        persisted = expectedPersisted.stream().filter(this::extractResourceCompleteness).collect(Collectors.toSet());
        LOGGER.debug("");

        // Then - resource service returned expected leaf resources
        expectedReturned.forEach(resource -> LOGGER.debug("Expected: {}", resource.getId()));
        returnedAuditable.forEach(response -> LOGGER.debug("Returned: {}", response.getResourceResponse().resource.getId()));
        assertThat(returnedRootDirRequest).isEqualTo(expectedReturned);
        LOGGER.debug("");

        // Then - persistence layer stored expected resources of all kinds
        expectedPersisted.forEach(resource -> LOGGER.debug("Expected:  {}", resource.getId()));
        persisted.forEach(resource -> LOGGER.debug("Persisted: {}", resource.getId()));
        assertThat(persisted).isEqualTo(expectedPersisted);
        LOGGER.debug("");
        LOGGER.debug("");
    }

    Boolean extractResourceCompleteness(final Resource resource) {
        return completenessRepository.findOneByEntityTypeAndEntityId(EntityType.RESOURCE, resource.getId()).blockOptional().isPresent();
    }
}
