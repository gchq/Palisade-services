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

import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.reader.common.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleResourceServiceTest {
    private final SimpleResourceService service = new SimpleResourceService("data-service", "java.lang.String");

    @Test
    void testJavaFilesInUnitTest() throws IOException {
        // Given
        var unitTestJava = (DirectoryResource) ResourceBuilder.create(new File("./src/unit-tests/java").getCanonicalFile().toURI());
        var resource = (LeafResource) ResourceBuilder.create(
                new File("./src/unit-tests/java/uk/gov/gchq/palisade/service/resource/ApplicationTestData.java").getCanonicalFile().toURI()
        );
        resource.type("java.lang.String").serialisedFormat("java").connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));
        Set<LeafResource> testFiles = new HashSet<>();

        // When
        service.getResourcesById(unitTestJava.getId()).forEachRemaining(testFiles::add);

        // Then
        assertThat(testFiles)
                .as("Check that when getting a resource by its ID, the correct resource is returned")
                .contains(resource);
    }

    @Test
    void testCanFindTestResourceAvro() throws IOException {
        // Given
        var avroFileURI = new File("./src/unit-tests/resources/test_resource.avro").getCanonicalFile().toURI();
        var testResourceAvro = (LeafResource) ResourceBuilder.create(avroFileURI);
        var testResourceDir = (DirectoryResource) testResourceAvro.getParent();

        // Given
        var expectedAvroResource = FunctionalIterator.fromIterator(service.query(avroFileURI, x -> true));
        var leafResource = expectedAvroResource.next();

        // When
        var resourcesById = FunctionalIterator.fromIterator(service.getResourcesById(testResourceDir.getId()))
                .filter(resource -> resource.getSerialisedFormat().equals("avro"));

        // Then
        assertThat(resourcesById.next())
                .as("Check that when getting resource by ID, the correct resource is returned")
                .isEqualTo(leafResource);

        // When
        var resourcesByFormat = FunctionalIterator.fromIterator(service.getResourcesBySerialisedFormat(leafResource.getSerialisedFormat()))
                .filter(resource -> resource.getId().equals(leafResource.getId()));

        assertThat(resourcesByFormat.next())
                .as("Check that when getting resource by its format, the correct resource is returned")
                .isEqualTo(leafResource);
    }
}
