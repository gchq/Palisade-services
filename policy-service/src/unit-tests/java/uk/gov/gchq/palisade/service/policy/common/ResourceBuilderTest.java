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
package uk.gov.gchq.palisade.service.policy.common;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.policy.common.resource.ChildResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.policy.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.policy.common.util.UriBuilder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceBuilderTest {

    @Test
    void testsInvalidSchemaThrowsException() throws URISyntaxException {
        // Given
        var uri = new URI("badschema:/path/to/resource");

        assertThat(ResourceBuilder.canCreate(uri))
                .as("Check the ResourceBuilder returns false")
                .isFalse();

        var exception = assertThrows(IllegalArgumentException.class, () -> ResourceBuilder.create(uri));

        assertThat(exception)
                .as("Check that the message attached appropriately describes the exception")
                .extracting(Throwable::getMessage)
                .isEqualTo("No enum constant uk.gov.gchq.palisade.service.policy.common.util.ResourceBuilder.Scheme.BADSCHEMA");
    }

    @Test
    void testShouldCreateSystemResource() {
        // A file schema uri for a system root should return a SystemResource
        // eg. "file:/" = System "/"

        // Given
        final File root = new File("/");

        // When
        SystemResource systemResource = (SystemResource) ResourceBuilder.create(root.toURI());

        // Then
        LinkedList<Resource> parents = getAllParents(systemResource);
        // System at the 'top'
        assertThat(parents.getFirst())
                .as("Check the parent has been created successfully")
                .isInstanceOf(SystemResource.class);

        parents.removeFirst();
        // Nothing else
        assertThat(parents)
                .as("Check that there are no resources")
                .isEmpty();
    }

    @Test
    void testShouldCreateDirectoryResource() {
        // A file schema uri for a directory should return a DirectoryResource
        // The parents of this DirectoryResource should be zero or more DirectoryResources up-to a root SystemResource
        // eg. "file:/dev/Palisade-common/" = System "/" -> Directory "/dev/" -> Directory "/dev/Palisade-common/"

        // Given
        final File userDir = new File(System.getProperty("user.dir"));

        // When
        DirectoryResource directoryResource = (DirectoryResource) ResourceBuilder.create(userDir.toURI());

        // Then
        LinkedList<Resource> parents = getAllParents(directoryResource);
        // System at the 'top'
        assertThat(parents.getFirst())
                .as("Check the parent has been created successfully")
                .isInstanceOf(SystemResource.class);

        parents.removeFirst();
        // Directories at the 'bottom'
        assertThat(parents)
                .allSatisfy(resource -> assertThat(resource)
                        .as("Check that all Resources have the correct hierarchy")
                        .isInstanceOf(DirectoryResource.class));
    }

    @Test
    void testShouldCreateFileResource() {
        // A file schema uri for a file should return a FileResource
        // The parents of this FileResource should be zero or more DirectoryResources up-to a root SystemResource
        // eg. "file:/dev/Palisade-common/pom.xml" = System "/" -> Directory "/dev/" -> Directory "/dev/Palisade-common/" -> File "/dev/Palisade-common/pom.xml"

        // Given
        final File pom = new File(System.getProperty("user.dir") + "/pom.xml");

        // When
        FileResource fileResource = (FileResource) ResourceBuilder.create(pom.toURI());

        // Then
        LinkedList<Resource> parents = getAllParents(fileResource);
        // System at the 'top'
        assertThat(parents.getFirst())
                .as("Check the parent has been created successfully")
                .isInstanceOf(SystemResource.class);

        parents.removeFirst();
        // File at the 'bottom'
        assertThat(parents.getLast())
                .as("Check the parent has been created successfully")
                .isInstanceOf(FileResource.class);
        parents.removeLast();
        // Directories in the 'middle'

        assertThat(parents)
                .allSatisfy(resource -> assertThat(resource)
                        .as("Check that all Resources have the correct hierarchy")
                        .isInstanceOf(DirectoryResource.class));
    }

    @Test
    void testShouldNormaliseRelativePaths() {
        // A file schema uri for a file with a relative path should return a FileResource with an absolute resource id
        // The parents of this FileResource should be zero or more DirectoryResources up-to a root SystemResource
        // eg. "file:/dev/Palisade-common/pom.xml" = System "/" -> Directory "/dev/" -> Directory "/dev/Palisade-common/" -> File "/dev/Palisade-common/pom.xml"

        // Given
        final URI absolutePom = new File(System.getProperty("user.dir") + "/pom.xml").toURI();
        final URI relativePom = UriBuilder.create(absolutePom)
                .withoutScheme()
                .withoutAuthority()
                .withPath(absolutePom.getPath() + "/../pom.xml")
                .withoutQuery()
                .withoutFragment();

        // When
        FileResource relativeFile = (FileResource) ResourceBuilder.create(relativePom);
        FileResource absoluteFile = (FileResource) ResourceBuilder.create(absolutePom);

        // Then
        assertThat(relativeFile)
                .as("Check the file has been structured correctly")
                .isEqualTo(absoluteFile);
    }

    private LinkedList<Resource> getAllParents(final Resource resource) {
        if (resource instanceof ChildResource) {
            final LinkedList<Resource> parents = getAllParents(((ChildResource) resource).getParent());
            parents.addLast(resource);
            return parents;
        } else {
            return new LinkedList<>(Collections.singleton(resource));
        }
    }
}
