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

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleResourceServiceTest {
    private final SimpleResourceService service = new SimpleResourceService("data-service", "java.lang.String");

    @Test
    public void testJavaFilesInSrc() throws IOException {
        // Given
        Set<LeafResource> javaFiles = service.getResourcesBySerialisedFormat("java").collect(Collectors.toSet());
        DirectoryResource srcMainJava = (DirectoryResource) ResourceBuilder.create(new File("./src/main/java").getCanonicalFile().toURI());
        DirectoryResource unitTestJava = (DirectoryResource) ResourceBuilder.create(new File("./src/unit-tests/java").getCanonicalFile().toURI());
        DirectoryResource compTestJava = (DirectoryResource) ResourceBuilder.create(new File("./src/component-tests/java").getCanonicalFile().toURI());
        DirectoryResource ctractTestJava = (DirectoryResource) ResourceBuilder.create(new File("./src/contract-tests/java").getCanonicalFile().toURI());

        // When
        Stream<LeafResource> sourceFiles = service.getResourcesById(srcMainJava.getId());
        Stream<LeafResource> testFiles = Stream.of(
                service.getResourcesById(unitTestJava.getId()),
                service.getResourcesById(compTestJava.getId()),
                service.getResourcesById(ctractTestJava.getId()))
                .flatMap(Function.identity());
        Set<LeafResource> srcAndTestJavaFiles = Stream.concat(sourceFiles, testFiles).collect(Collectors.toSet());

        // Then
        assertThat(javaFiles).isEqualTo(srcAndTestJavaFiles);
    }

    @Test
    public void testCanFindTestResourceAvro() throws IOException {
        // Given
        URI avroFileURI = new File("./src/unit-tests/resources/test_resource.avro").getCanonicalFile().toURI();
        FileResource testResourceAvro = (FileResource) ResourceBuilder.create(avroFileURI);
        DirectoryResource testResourceDir = (DirectoryResource) testResourceAvro.getParent();

        // Given
        LeafResource expectedAvroResource = service.query(avroFileURI, x -> true).findFirst().orElseThrow();

        // When
        Optional<LeafResource> resourcesById = service.getResourcesById(testResourceDir.getId())
                .filter(expectedAvroResource::equals)
                .findFirst();

        // Then
        assertThat(resourcesById).isPresent();
        assertThat(resourcesById.get()).isEqualTo(expectedAvroResource);

        // When
        Optional<LeafResource> resourcesByType = service.getResourcesBySerialisedFormat(expectedAvroResource.getSerialisedFormat())
                .filter(expectedAvroResource::equals)
                .findFirst();

        assertThat(resourcesByType).isPresent();
        assertThat(resourcesByType.get()).isEqualTo(expectedAvroResource);
    }
}
