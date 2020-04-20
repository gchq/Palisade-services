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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SimpleResourceServiceTest {
    private final SimpleResourceService service = new SimpleResourceService();

    @Test
    public void javaFilesInSrcAndTest() {
        // Given
        Set<LeafResource> javaFiles = service.getResourcesByType("java").collect(Collectors.toSet());

        // When
        Stream<LeafResource> sourceFiles = service.getResourcesById("./src/main/java");
        Stream<LeafResource> testFiles = service.getResourcesById("./src/test/java");
        Set<LeafResource> srcAndTestJavaFiles = Stream.concat(sourceFiles, testFiles).collect(Collectors.toSet());

        // Then
        assertThat(javaFiles, equalTo(srcAndTestJavaFiles));
    }

    @Test
    public void canFindTestResourceAvro() {
        // Given
        LeafResource testResourceAvro = service.query("./src/test/resources/test_resource.avro", x -> true).findAny().orElseThrow();

        // When
        Optional<LeafResource> resourcesById = service.getResourcesById("./src/test/resources").findFirst();
        Optional<LeafResource> resourcesByType = service.getResourcesByType("avro").findFirst();

        // Then
        assertTrue(resourcesById.isPresent());
        assertThat(resourcesById.get(), equalTo(testResourceAvro));

        assertTrue(resourcesByType.isPresent());
        assertThat(resourcesByType.get(), equalTo(testResourceAvro));
    }
}
