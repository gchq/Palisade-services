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

package uk.gov.gchq.palisade.component.resource.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"dbtest", "web"})
class JpaPersistenceLayerTest {
    @Autowired
    private JpaPersistenceLayer persistenceLayer;
    private LeafResource resource;

    @BeforeEach
    @Transactional
    public void setUp() {
        // Given
        resource = ((FileResource) ResourceBuilder.create("file:/root/test-file-id"))
                .type("type")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));

        // addResource is only appropriate for runtime updates to an existing set, whereas put is appropriate for initialisation
        FunctionalIterator<LeafResource> resourceIterator = persistenceLayer.withPersistenceById(resource.getParent().getId(),
                FunctionalIterator.fromIterator(Collections.singletonList(resource).iterator()));
        resourceIterator = persistenceLayer.withPersistenceByType(resource.getType(), resourceIterator);
        resourceIterator = persistenceLayer.withPersistenceBySerialisedFormat(resource.getSerialisedFormat(), resourceIterator);
        while (resourceIterator.hasNext()) {
            resourceIterator.next();
        }
    }

    @Test
    void testSpringDiscoversJpaPersistenceLayer() {
        // When the spring application is started
        // Then
        assertThat(persistenceLayer).isNotNull();
    }

    @Test
    @Transactional
    void testEmptyGetReturnsEmpty() {
        // When getting a non-existent resourceId
        FunctionalIterator<LeafResource> persistenceResponse = FunctionalIterator.fromIterator(
                persistenceLayer.getResourcesById("file:/NON_EXISTENT_RESOURCE_ID"));
        // Then the iterator should be empty
        assertThat(persistenceResponse.hasNext()).isFalse();

        // When getting a non-existent resource type
        persistenceResponse = FunctionalIterator.fromIterator(
                persistenceLayer.getResourcesByType("NON_EXISTENT_RESOURCE_TYPE"));
        // Then the iterator should be empty
        assertThat(persistenceResponse.hasNext()).isFalse();

        // When getting a non-existent resource serialised format
        persistenceResponse = FunctionalIterator.fromIterator(
                persistenceLayer.getResourcesBySerialisedFormat("NON_EXISTENT_RESOURCE_FORMAT"));
        // Then the iterator should be empty
        assertThat(persistenceResponse.hasNext()).isFalse();
    }

    @Test
    @Transactional
    void testAddAndGetReturnsResource() {
        // Given the setup

        // When getting a resource from the persistence layer by resourceId
        FunctionalIterator<LeafResource> persistenceResponse = FunctionalIterator.fromIterator(persistenceLayer.getResourcesById(resource.getId()));
        // Then the returned resource should match the created resource
        assertThat(persistenceResponse.next()).isEqualTo(resource);

        // When getting a resource from the persistence layer by type
        persistenceResponse = FunctionalIterator.fromIterator(persistenceLayer.getResourcesByType(resource.getType()));
        // Then the returned resource should match the created resource
        assertThat(persistenceResponse.next()).isEqualTo(resource);

        // When getting a resource from the persistence layer by serialised format
        persistenceResponse = FunctionalIterator.fromIterator(persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat()));
        // Then the returned resource should match the created resource
        assertThat(persistenceResponse.next()).isEqualTo(resource);
    }
}
