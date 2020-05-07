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

package uk.gov.gchq.palisade.service.resource.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.resource.config.ClientConfiguration;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SimpleResourceServiceTest {
    private final ClientConfiguration clientConfiguration = Mockito.mock(ClientConfiguration.class);
    private final SimpleResourceService service = new SimpleResourceService(clientConfiguration);

    @Before
    public void setUp() throws URISyntaxException {
        Mockito.when(clientConfiguration.getClientUri(Mockito.anyString())).thenReturn(Optional.of(new URI("http://data-service-uri")));
    }

    @Test
    public void javaFilesInSrcAndTest() throws IOException {
        // Given
        Set<LeafResource> javaFiles = service.getResourcesBySerialisedFormat("java").collect(Collectors.toSet());
        DirectoryResource srcMainJava = (DirectoryResource) ResourceBuilder.create(new File("./src/main/java").getCanonicalFile().toURI());
        DirectoryResource srcTestJava = (DirectoryResource) ResourceBuilder.create(new File("./src/test/java").getCanonicalFile().toURI());

        // When
        Stream<LeafResource> sourceFiles = service.getResourcesById(srcMainJava.getId());
        Stream<LeafResource> testFiles = service.getResourcesById(srcTestJava.getId());
        Set<LeafResource> srcAndTestJavaFiles = Stream.concat(sourceFiles, testFiles).collect(Collectors.toSet());

        // Then
        assertThat(javaFiles, equalTo(srcAndTestJavaFiles));
    }

    @Test
    public void canFindTestResourceAvro() throws IOException {
        // Given
        URI avroFileURI = new File("./src/test/resources/test_resource.avro").getCanonicalFile().toURI();
        FileResource testResourceAvro = (FileResource) ResourceBuilder.create(avroFileURI);
        DirectoryResource testResourceDir = (DirectoryResource) testResourceAvro.getParent();

        // Given
        LeafResource expectedAvroResource = service.query(avroFileURI, x -> true).findFirst().orElseThrow();

        // When
        Optional<LeafResource> resourcesById = service.getResourcesById(testResourceDir.getId())
                .filter(expectedAvroResource::equals)
                .findFirst();

        // Then
        assertTrue(resourcesById.isPresent());
        assertThat(resourcesById.get(), equalTo(expectedAvroResource));

        // When
        Optional<LeafResource> resourcesByType = service.getResourcesBySerialisedFormat(expectedAvroResource.getSerialisedFormat())
                .filter(expectedAvroResource::equals)
                .findFirst();

        assertTrue(resourcesByType.isPresent());
        assertThat(resourcesByType.get(), equalTo(expectedAvroResource));
    }
}
