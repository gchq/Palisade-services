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
import akka.stream.javadsl.Keep;
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

import uk.gov.gchq.palisade.service.resource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.resource.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.config.DefaultConfiguration;
import uk.gov.gchq.palisade.service.resource.config.R2dbcConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.ReactivePersistenceLayer;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ContextConfiguration(classes = {ApplicationConfiguration.class, DefaultConfiguration.class, R2dbcConfiguration.class, AkkaSystemConfig.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableR2dbcRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"db-test"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ReactivePersistenceLayerTest {

    @Autowired
    private ReactivePersistenceLayer persistenceLayer;
    @Autowired
    private Materializer materializer;

    private LeafResource resource;

    @BeforeEach
    public void setUp() throws InterruptedException {
        // Given
        resource = ((FileResource) ResourceBuilder.create("file:/root/test-file-id"))
                .type("test-type")
                .serialisedFormat("test-format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));

        // addResource is only appropriate for runtime updates to an existing set, whereas put is appropriate for initialisation
        Source.single(resource)
                .via(persistenceLayer.withPersistenceById(resource.getId()))
                .via(persistenceLayer.withPersistenceByType(resource.getType()))
                .via(persistenceLayer.withPersistenceBySerialisedFormat(resource.getSerialisedFormat()))
                .runWith(Sink.ignore(), materializer);

        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void testSpringDiscoversPersistenceLayer() {
        // When the spring application is started
        assertThat(persistenceLayer)
                .as("Check the persistenceLayer has been autowired successfully")
                .isNotNull();
    }

    @Test
    void testEmptyGetReturnsEmpty() {
        // Given the setup

        // When getting a non-existent resourceId
        var persistenceIdResponse = persistenceLayer.getResourcesById("file:/NON_EXISTENT_RESOURCE_ID").join();
        // Then the list should be empty
        assertThat(persistenceIdResponse)
                .as("Check that no resources are returned for a non existent resourceId")
                .isEmpty();

        // When getting a non-existent resource type
        var persistenceTypeResponse = persistenceLayer.getResourcesByType("NON_EXISTENT_RESOURCE_TYPE").join();
        // Then the list should be empty
        assertThat(persistenceTypeResponse)
                .as("Check that no resources are returned for a non existent resource type")
                .isEmpty();

        // When getting a non-existent resource serialised format
        var persistenceFormatResponse = persistenceLayer.getResourcesBySerialisedFormat("NON_EXISTENT_RESOURCE_FORMAT").join();
        // Then the list should be empty
        assertThat(persistenceFormatResponse)
                .as("Check that no resources are returned for a non existent resource format")
                .isEmpty();
    }

    @Test
    void testAddAndGetReturnsResource() {
        // Given the setup

        // When getting a resource from the persistence layer by resourceId
        var idResult = persistenceLayer.getResourcesById(resource.getId())
                .join().orElseThrow()
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        // Then the returned resource should match the created resource
        assertThat(idResult)
                .as("Check that when getting a resource by its Id, the correct resource is returned")
                .containsOnly(resource);

        // When getting a resource from the persistence layer by type
        var typeResult = persistenceLayer.getResourcesByType(resource.getType())
                .join().orElseThrow()
                .toMat(Sink.seq(), Keep.right()).run(materializer)
                .toCompletableFuture().join();
        // Then the returned resource should match the created resource
        assertThat(typeResult)
                .as("Check that when getting the resource by its type, the correct resource is returned")
                .containsOnly(resource);

        // When getting a resource from the persistence layer by serialised format
        var formatResult = persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat())
                .join().orElseThrow()
                .runWith(Sink.seq(), materializer)
                .toCompletableFuture().join();
        // Then the returned resource should match the created resource
        assertThat(formatResult)
                .as("Check that when geting the resource by its format, the correct resource is returned")
                .containsOnly(resource);
    }
}
