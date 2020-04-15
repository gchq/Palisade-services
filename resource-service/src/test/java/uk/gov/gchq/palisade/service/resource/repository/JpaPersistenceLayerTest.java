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

package uk.gov.gchq.palisade.service.resource.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dbtest")
public class JpaPersistenceLayerTest {
    // No DirtiesContext as a restart is slow

    @Autowired
    private JpaPersistenceLayer persistenceLayer;
    private LeafResource resource;

    @Before
    public void setUp() {
        // Given
        resource = new FileResource()
                .id("/test-file-id")
                .type("type")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().uri("data-service"))
                .parent(new SystemResource().id("/"));
        // addResource is only appropriate for runtime updates to an existing set, whereas put is appropriate for initialisation
        persistenceLayer.putResourcesById("", resource);
        persistenceLayer.putResourcesByType(resource.getType(), resource);
        persistenceLayer.putResourcesBySerialisedFormat(resource.getSerialisedFormat(), resource);
    }

    @Test
    public void springDiscoversJpaPersistenceLayerTest() {
        // When the spring application is started
        // Then
        assertThat(persistenceLayer, notNullValue());
    }

    @Test
    @Transactional
    public void emptyGetReturnsEmptyTest() {
        // When
        Optional<Stream<LeafResource>> persistenceResponse = persistenceLayer.getResourcesById("NON_EXISTENT_RESOURCE_ID");
        // Then
        assertThat(persistenceResponse, equalTo(Optional.empty()));

        // When
        persistenceResponse = persistenceLayer.getResourcesByType("NON_EXISTENT_RESOURCE_TYPE");
        // Then
        assertThat(persistenceResponse, equalTo(Optional.empty()));

        // When
        persistenceResponse = persistenceLayer.getResourcesBySerialisedFormat("NON_EXISTENT_RESOURCE_FORMAT");
        // Then
        assertThat(persistenceResponse, equalTo(Optional.empty()));
    }

    @Test
    @Transactional
    public void addAndGetReturnsResourceTest() {
        // When
        Optional<Stream<LeafResource>> persistenceResponse = persistenceLayer.getResourcesById(resource.getId());
        // Then
        Stream<LeafResource> resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet()), equalTo(Collections.singleton(resource)));

        // When
        persistenceResponse = persistenceLayer.getResourcesByType(resource.getType());
        // Then
        resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet()), equalTo(Collections.singleton(resource)));

        // When
        persistenceResponse = persistenceLayer.getResourcesBySerialisedFormat(resource.getSerialisedFormat());
        // Then
        resourceStream = persistenceResponse.orElseThrow();
        assertThat(resourceStream.collect(Collectors.toSet()), equalTo(Collections.singleton(resource)));
    }
}
