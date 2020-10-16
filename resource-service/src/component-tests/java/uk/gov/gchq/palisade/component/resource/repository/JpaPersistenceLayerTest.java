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
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ContextConfiguration(classes = {ApplicationConfiguration.class})
@EntityScan(basePackages = {"uk.gov.gchq.palisade.service.resource.domain"})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.resource.repository"})
@ActiveProfiles({"h2", "web"})
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
        persistenceLayer.withPersistenceById(resource.getParent().getId(), Stream.of(resource)).forEach(x -> {
        });
        persistenceLayer.withPersistenceByType(resource.getType(), Stream.of(resource)).forEach(x -> {
        });
        persistenceLayer.withPersistenceBySerialisedFormat(resource.getSerialisedFormat(), Stream.of(resource)).forEach(x -> {
        });
    }

    @Test
    void testSpringDiscoversJpaPersistenceLayer() {
        // When the spring application is started
        // Then
        assertThat(persistenceLayer).isNotNull();
    }

    @Test
    @Transactional
    public void testEmptyGetReturnsEmpty() {
        // When
        Optional<Stream<LeafResource>> persistenceResponse = persistenceLayer.getResourcesById("file:/NON_EXISTENT_RESOURCE_ID");
        // Then
        assertThat(persistenceResponse).isEmpty();

        // When
        persistenceResponse = persistenceLayer.getResourcesByType("NON_EXISTENT_RESOURCE_TYPE");
        // Then
        assertThat(persistenceResponse).isEmpty();

        // When
        persistenceResponse = persistenceLayer.getResourcesBySerialisedFormat("NON_EXISTENT_RESOURCE_FORMAT");
        // Then
        assertThat(persistenceResponse).isEmpty();
    }

    @Test
    @Transactional
    public void testAddAndGetReturnsResource() {
        // When
        Optional<Stream<LeafResource>> persistenceResponse = persistenceLayer.getResourcesById(resource.getId());
        // Then
        Stream<LeafResource> resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet())).isEqualTo(Collections.singleton(resource));

        // When
        persistenceResponse = persistenceLayer.getResourcesByType(resource.getType());
        // Then
        resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet())).isEqualTo(Collections.singleton(resource));

        // When
        persistenceResponse = persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat());
        // Then
        resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet())).isEqualTo(Collections.singleton(resource));
    }
}
