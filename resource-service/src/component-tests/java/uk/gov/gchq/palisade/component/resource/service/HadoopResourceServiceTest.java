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

package uk.gov.gchq.palisade.component.resource.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.resource.ChildResource;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.ParentResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.reader.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.util.HadoopResourceDetails;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HadoopResourceServiceTest {

    private static final String FORMAT_VALUE = "txt";
    private static final String TYPE_VALUE = "bob";
    private static final String TYPE_CLASSNAME = "com.type.bob";
    private static final String FILE_NAME_VALUE_00001 = "00001";
    private static final String FILE_NAME_VALUE_00002 = "00002";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");
    private static final String HDFS = "hdfs";

    @TempDir
    protected File tmpDirectory;
    private URI id1;
    private URI id2;
    private LeafResource resource1;
    private LeafResource resource2;
    private URI root;
    private URI dir;
    private FileSystem fs;
    private Configuration config = new Configuration();
    private HadoopResourceService resourceService;

    private static String getFileNameFromResourceDetails(final String name, final String type, final String format) {
        //Type, Id, Format
        return type + "_" + name + "." + format;
    }

    @BeforeEach
    void setup() throws IOException {
        if (IS_WINDOWS) {
            System.setProperty("hadoop.home.dir", Paths.get("./src/test/resources/hadoop-3.2.1").toAbsolutePath().normalize().toString());
        }
        fs = FileSystem.get(config);
        root = tmpDirectory.getAbsoluteFile().toURI();
        dir = root.resolve("inputDir/");
        config = createConf(root.toString());
        fs.mkdirs(new Path(dir));
        writeFile(fs, dir, FILE_NAME_VALUE_00001, FORMAT_VALUE, TYPE_VALUE);
        writeFile(fs, dir, FILE_NAME_VALUE_00002, FORMAT_VALUE, TYPE_VALUE);

        var connectionDetail = new SimpleConnectionDetail().serviceName("data-service-mock");
        id1 = dir.resolve(getFileNameFromResourceDetails(FILE_NAME_VALUE_00001, TYPE_VALUE, FORMAT_VALUE));
        resource1 = ((LeafResource) ResourceBuilder.create(id1))
                .type(TYPE_CLASSNAME)
                .serialisedFormat(FORMAT_VALUE)
                .connectionDetail(connectionDetail);
        id2 = dir.resolve(getFileNameFromResourceDetails(FILE_NAME_VALUE_00002, TYPE_VALUE, FORMAT_VALUE));
        resource2 = ((LeafResource) ResourceBuilder.create(id2))
                .type(TYPE_CLASSNAME)
                .serialisedFormat(FORMAT_VALUE)
                .connectionDetail(connectionDetail);

        resourceService = new HadoopResourceService(config);
        resourceService.addDataService(connectionDetail);
        HadoopResourceDetails.addTypeSupport(TYPE_VALUE, TYPE_CLASSNAME);
    }

    @Test
    void testGetResourcesById() {
        // Given an empty list
        List<LeafResource> resultList = new ArrayList<>();

        // When making a get request to the resource service by resourceId
        final Iterator<LeafResource> resourcesById = resourceService.getResourcesById(id1.toString());
        resourcesById.forEachRemaining(resultList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resultList)
                .as("Check that when I get a resource by its Id, the correct resource is returned")
                .containsOnly(resource1);
    }

    @Test
    void testShouldGetResourcesOutsideOfScope() throws URISyntaxException {
        // Given setup

        // When making a get request to the resource service by resourceId
        final URI found = new URI(HDFS, "/unknownDir" + id1.getPath(), null);
        var exception = assertThrows(Exception.class,
                () -> resourceService.getResourcesById(found.toString()), HadoopResourceService.ERROR_OUT_SCOPE);

        var outOfScope = String.format(HadoopResourceService.ERROR_OUT_SCOPE, found, config.get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY));
        // Then assert the expected error message is returned
        assertThat(outOfScope)
                .as("Check that when I try to get a resource in an unknown directory, the correct error message is thrown")
                .isEqualTo(exception.getMessage());
    }

    @Test
    void testShouldGetResourcesByIdOfAFolder() {
        // Given an empty list
        List<LeafResource> resultList = new ArrayList<>();

        // When making a get request to the resource service by resourceId
        final Iterator<LeafResource> resourcesById = resourceService.getResourcesById(dir.toString());
        resourcesById.forEachRemaining(resultList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resultList)
                .as("Check that when I get resources by the Id of the directory, the correct resources are returned")
                .containsOnly(resource1, resource2);
    }

    @Test
    void testShouldFilterOutIllegalFileName() throws Exception {
        // Given an illegal file is added
        List<LeafResource> resourceList = new ArrayList<>();
        writeFile(fs, dir.resolve("./I-AM-AN-ILLEGAL-FILENAME"));

        // When making a get request to the resource service by resourceId
        final Iterator<LeafResource> resourcesById = resourceService.getResourcesById(dir.toString());
        resourcesById.forEachRemaining(resourceList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resourceList)
                .as("Check that when I get resources by the Id of the directory, the correct resources are returned")
                .containsOnly(resource1, resource2);
    }

    @Test
    void testShouldGetResourcesByType() throws Exception {
        // Given a new file with a new type is added
        List<LeafResource> resultList = new ArrayList<>();
        writeFile(fs, dir, "00003", FORMAT_VALUE, "not" + TYPE_VALUE);
        HadoopResourceDetails.addTypeSupport("not" + TYPE_VALUE, TYPE_CLASSNAME + ".not");

        // When making a get request to the resource service by type
        final Iterator<LeafResource> resourcesByType = resourceService.getResourcesByType(TYPE_CLASSNAME);
        resourcesByType.forEachRemaining(resultList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resultList)
                .as("Check that when I get resources by their type, the correct resources are returned")
                .containsOnly(resource1, resource2);
    }

    @Test
    void testShouldGetResourcesByFormat() throws Exception {
        // Given a new file with a new format is added
        List<LeafResource> resultList = new ArrayList<>();
        writeFile(fs, dir, "00003", "not" + FORMAT_VALUE, TYPE_VALUE);

        // When making a get request to the resource service by serialisedFormat
        final Iterator<LeafResource> resourcesBySerialisedFormat = resourceService.getResourcesBySerialisedFormat(FORMAT_VALUE);
        resourcesBySerialisedFormat.forEachRemaining(resultList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resultList)
                .as("Check that when I get resources by their format, the correct resources are returned")
                .containsOnly(resource1, resource2);

    }

    @Test
    void testShouldGetResourcesByResource() {
        // Given an empty list
        List<LeafResource> resultList = new ArrayList<>();

        // When making a get request to the resource service by resource
        final Iterator<LeafResource> resourcesByResource = resourceService.getResourcesByResource(new DirectoryResource().id(dir.toString()));
        resourcesByResource.forEachRemaining(resultList::add);

        // Then assert that the expected resource(s) are returned
        assertThat(resultList)
                .as("Check that when I get resources by the same resource, the correct resources are returned")
                .containsOnly(resource1, resource2);
    }

    @Test
    void testAddResource() {
        assertThat(resourceService.addResource(null))
                .as("Check that when adding a invalid resource, the correct response is returned")
                .isFalse();
    }

    @Test
    void testShouldResolveParents() {
        final URI id = dir.resolve("folder1/folder2/" + getFileNameFromResourceDetails(FILE_NAME_VALUE_00001, TYPE_VALUE, FORMAT_VALUE));
        final FileResource fileResource = (FileResource) ResourceBuilder.create(id);

        final ParentResource parent1 = fileResource.getParent();

        assertThat(dir.resolve("folder1/folder2/").toString())
                .as("Check the URI of the parent is the correct URI")
                .isEqualTo(parent1.getId());

        assertThat(parent1)
                .as("Check that the ParentResource has been instantiated correctly")
                .isInstanceOf(ChildResource.class)
                .isInstanceOf(DirectoryResource.class);

        final ChildResource child = (ChildResource) parent1;
        final ParentResource parent2 = child.getParent();

        assertThat(dir.resolve("folder1/").toString())
                .as("Check that the URI of the parent is the correct URI")
                .isEqualTo(parent2.getId());

        assertThat(parent2)
                .as("Check that the ParentResource has been instantiated correctly")
                .isInstanceOf(ChildResource.class)
                .isInstanceOf(DirectoryResource.class);

        final ChildResource child2 = (ChildResource) parent2;

        final ParentResource parent3 = child2.getParent();

        assertThat(dir.toString())
                .as("Check that the URI returned is the correct parent id")
                .isEqualTo(parent3.getId());

        assertThat(parent3)
                .as("Check that the ParentResource has been instantiated correctly")
                .isInstanceOf(ChildResource.class)
                .isInstanceOf(DirectoryResource.class);


        final ChildResource child3 = (ChildResource) parent3;
        final ParentResource parent4 = child3.getParent();

        assertThat(root.toString())
                .as("Check the URI of the root is correct")
                .isEqualTo(parent4.getId());
    }

    private Configuration createConf(final String fsDefaultName) {
        // Set up local conf
        final Configuration conf = new Configuration();
        conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, fsDefaultName);
        return conf;
    }

    private void writeFile(final FileSystem fs, final URI parentPath, final String name, final String format, final String type) throws IOException {
        writeFile(fs, parentPath.resolve(getFileNameFromResourceDetails(name, type, format)));
    }

    private void writeFile(final FileSystem fs, final URI filePathURI) throws IOException {
        //Write Some file
        final Path filePath = new Path(filePathURI);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(filePath, true)))) {
            writer.write("myContents");
        }
    }
}
