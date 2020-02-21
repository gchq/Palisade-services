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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class SimpleResourceServiceTest {

    private static LeafResource resource = new FileResource().id("/path/test_file.txt").type("test").serialisedFormat("txt");

    private Map<LeafResource, ConnectionDetail> resourceMap = new HashMap<>();
    private SimpleConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("localhost");

    @Mock
    SimpleResourceService resourceService;

    @Before
    public void setUp() {
        resourceMap.put(resource, connectionDetail);
    }

    @Test
    public void getResourceByResourceTest() {
        // Given
        Map<LeafResource, ConnectionDetail> expected = new HashMap<>();
        expected.put(resource, connectionDetail);
        Mockito.when(resourceService.getResourcesByResource(resource)).thenReturn(resourceMap);

        // When
        Map<LeafResource, ConnectionDetail> actual = resourceService.getResourcesByResource(resource);

        // Then
        Assert.assertThat(actual.containsKey(resource), is(expected.containsKey(resource)));
    }

    @Test
    public void getResourceByIdTest() {
        // Given
        Map<LeafResource, ConnectionDetail> expected = new HashMap<>();
        expected.put(resource, connectionDetail);
        Mockito.when(resourceService.getResourcesById("/path/test_file.txt")).thenReturn(resourceMap);

        // When
        Map<LeafResource, ConnectionDetail> actual = resourceService.getResourcesById("/path/test_file.txt");

        // Then
        Assert.assertThat(actual.containsKey(resource), is(expected.containsKey(resource)));
    }

    @Test
    public void getResourceByTypeTest() {
        // Given
        Map<LeafResource, ConnectionDetail> expected = new HashMap<>();
        expected.put(resource, connectionDetail);
        Mockito.when(resourceService.getResourcesByType("test")).thenReturn(resourceMap);

        // When
        Map<LeafResource, ConnectionDetail> actual = resourceService.getResourcesByType("test");

        // Then
        Assert.assertThat(actual.containsKey(resource), is(expected.containsKey(resource)));
    }

    @Test
    public void getResourceByFormat() {
        // Given
        Map<LeafResource, ConnectionDetail> expected = new HashMap<>();
        expected.put(resource, connectionDetail);
        Mockito.when(resourceService.getResourcesBySerialisedFormat("txt")).thenReturn(resourceMap);

        // When
        Map<LeafResource, ConnectionDetail> actual = resourceService.getResourcesBySerialisedFormat("txt");

        // Then
        Assert.assertThat(actual.containsKey(resource), is(expected.containsKey(resource)));
    }
}
