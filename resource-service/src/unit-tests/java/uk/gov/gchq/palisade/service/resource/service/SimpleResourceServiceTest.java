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

package uk.gov.gchq.palisade.service.resource.service;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.util.AbstractResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleResourceServiceTest {
    private final SimpleResourceService service = new SimpleResourceService("data-service", "java.lang.String");

    @Test
    void testJavaFilesInUnitTest() throws IOException {
        // Given we create a file and add it to the service
        DirectoryResource unitTestJava = (DirectoryResource) AbstractResourceBuilder.create("file:./src/unit-tests/java/");

        LeafResource resource = (LeafResource) AbstractResourceBuilder.create(
                new File("./src/unit-tests/java/uk/gov/gchq/palisade/service/resource/ApplicationTestData.java").getCanonicalFile().toURI());
        resource.type("java.lang.String")
                .serialisedFormat("java")
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));
        Set<LeafResource> testFiles = new HashSet<>();

        // When we get the file by the ID of the directory
        service.getResourcesById(unitTestJava.getId()).forEachRemaining(testFiles::add);

        // Then its been retrieved successfully
        assertThat(testFiles)
                .as("Check that the returned file is in the list returned")
                .contains(resource);
    }

    @Test
    void testCanFindTestResourceAvro() throws IOException {
        // Given we create a avro file
        URI avroFileURI = new File("./src/unit-tests/resources/test_resource.avro").getCanonicalFile().toURI();
        LeafResource testResourceAvro = (LeafResource) AbstractResourceBuilder.create(avroFileURI);
        DirectoryResource testResourceDir = (DirectoryResource) testResourceAvro.getParent();

        // Given we add the avro to our list of expected returned files
        FunctionalIterator<LeafResource> expectedAvroResource = FunctionalIterator.fromIterator(service.query(avroFileURI, x -> true));
        LeafResource leafResource = expectedAvroResource.next();

        // When we request the avro by passing in the id of its directory
        FunctionalIterator<LeafResource> resourcesById = FunctionalIterator.fromIterator(service.getResourcesById(testResourceDir.getId()))
                .filter(resource -> resource.getSerialisedFormat().equals("avro"));

        // Then it has been retrieved successfully
        assertThat(resourcesById.next())
                .as("Check that the file has been returned successfully")
                .usingRecursiveComparison()
                .isEqualTo(leafResource);

        // When we get the resource by its serialised format
        FunctionalIterator<LeafResource> resourcesByFormat = FunctionalIterator.fromIterator(service.getResourcesBySerialisedFormat(leafResource.getSerialisedFormat()))
                .filter(resource -> resource.getId().equals(leafResource.getId()));

        assertThat(resourcesByFormat.next())
                .as("Check that the file has been returned successfully")
                .usingRecursiveComparison()
                .isEqualTo(leafResource);
    }
}
