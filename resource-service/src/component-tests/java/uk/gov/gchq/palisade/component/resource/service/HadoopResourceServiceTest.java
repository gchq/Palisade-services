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

package uk.gov.gchq.palisade.component.resource.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.test.PathUtils;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.util.HadoopResourceDetails;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HadoopResourceServiceTest {

    private static final String FORMAT_VALUE = "txt";
    private static final String TYPE_VALUE = "bob";
    private static final String TYPE_CLASSNAME = "com.type.bob";
    private static final String FILE_NAME_VALUE_00001 = "00001";
    private static final String FILE_NAME_VALUE_00002 = "00002";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");
    private static final String HDFS = "hdfs";
    private static final File TMP_DIRECTORY;

    static {
        TMP_DIRECTORY = PathUtils.getTestDir(HadoopResourceServiceTest.class);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder(TMP_DIRECTORY);
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
    public void setup() throws IOException, URISyntaxException {
        if (IS_WINDOWS) {
            System.setProperty("hadoop.home.dir", Paths.get("./src/test/resources/hadoop-3.2.1").toAbsolutePath().normalize().toString());
        }
        fs = FileSystem.get(config);
        root = testFolder.getRoot().getAbsoluteFile().toURI();
        dir = root.resolve("inputDir/");
        config = createConf(root.toString());
        fs.mkdirs(new Path(dir));
        writeFile(fs, dir, FILE_NAME_VALUE_00001, FORMAT_VALUE, TYPE_VALUE);
        writeFile(fs, dir, FILE_NAME_VALUE_00002, FORMAT_VALUE, TYPE_VALUE);

        ConnectionDetail connectionDetail = new SimpleConnectionDetail().serviceName("data-service-mock");
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
    public void testGetResourcesById() {
        //given

        //when
        final Stream<LeafResource> resourcesById = resourceService.getResourcesById(id1.toString());

        //then
        Set<LeafResource> expected = Collections.singleton(resource1);
        assertThat(resourcesById.collect(Collectors.toSet())).isEqualTo(expected);
    }

    @Test
    public void testShouldGetResourcesOutsideOfScope() throws URISyntaxException {
        //given

        //when
        final URI found = new URI(HDFS, "/unknownDir" + id1.getPath(), null);
        try {
            resourceService.getResourcesById(found.toString());
            fail("exception expected");
        } catch (Exception e) {
            //then
            assertThat(String.format(HadoopResourceService.ERROR_OUT_SCOPE, found, config.get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY))).isEqualTo((e.getMessage()));
        }
    }

    @Test
    public void testShouldGetResourcesByIdOfAFolder() {
        //given

        //when
        final Stream<LeafResource> resourcesById = resourceService.getResourcesById(dir.toString());

        //then
        Set<LeafResource> expected = new HashSet<>(Arrays.asList(resource1, resource2));
        assertThat(resourcesById.collect(Collectors.toSet())).isEqualTo(expected);
    }

    @Test
    public void testShouldFilterOutIllegalFileName() throws Exception {
        //given
        writeFile(fs, dir.resolve("./I-AM-AN-ILLEGAL-FILENAME"));

        //when
        final Stream<LeafResource> resourcesById = resourceService.getResourcesById(dir.toString());

        //then
        Set<LeafResource> expected = new HashSet<>(Arrays.asList(resource1, resource2));
        assertThat(resourcesById.collect(Collectors.toSet())).isEqualTo(expected);
    }

    @Test
    public void testShouldGetResourcesByType() throws Exception {
        //given
        writeFile(fs, dir, "00003", FORMAT_VALUE, "not" + TYPE_VALUE);
        HadoopResourceDetails.addTypeSupport("not" + TYPE_VALUE, TYPE_CLASSNAME + ".not");

        //when
        final Stream<LeafResource> resourcesByType = resourceService.getResourcesByType(TYPE_CLASSNAME);

        //then
        Set<LeafResource> expected = new HashSet<>(Arrays.asList(resource1, resource2));
        assertThat(resourcesByType.collect(Collectors.toSet())).isEqualTo(expected);
    }

    @Test
    public void testShouldGetResourcesByFormat() throws Exception {
        //given
        writeFile(fs, dir, "00003", "not" + FORMAT_VALUE, TYPE_VALUE);

        //when
        final Stream<LeafResource> resourcesBySerialisedFormat = resourceService.getResourcesBySerialisedFormat(FORMAT_VALUE);

        //then
        Set<LeafResource> expected = new HashSet<>(Arrays.asList(resource1, resource2));
        assertThat(resourcesBySerialisedFormat.collect(Collectors.toSet())).isEqualTo(expected);

    }

    @Test
    public void testShouldGetResourcesByResource() {
        //given

        //when
        final Stream<LeafResource> resourcesByResource = resourceService.getResourcesByResource(new DirectoryResource().id(dir.toString()));

        //then
        Set<LeafResource> expected = new HashSet<>(Arrays.asList(resource1, resource2));
        assertThat(resourcesByResource.collect(Collectors.toSet())).isEqualTo(expected);
    }

    @Test
    public void testAddResource() {
        boolean success = resourceService.addResource(null);
        assertThat(success).isFalse();
    }

    @Test
    public void testShouldResolveParents() {
        final URI id = dir.resolve("folder1/folder2/" + getFileNameFromResourceDetails(FILE_NAME_VALUE_00001, TYPE_VALUE, FORMAT_VALUE));
        final FileResource fileResource = (FileResource) ResourceBuilder.create(id);

        final ParentResource parent1 = fileResource.getParent();

        assertThat(dir.resolve("folder1/folder2/").toString()).isEqualTo(parent1.getId());
        assertThat(parent1).isInstanceOf(ChildResource.class);
        assertThat(parent1).isInstanceOf(DirectoryResource.class);

        final ChildResource child = (ChildResource) parent1;

        final ParentResource parent2 = child.getParent();

        assertThat(dir.resolve("folder1/").toString()).isEqualTo(parent2.getId());
        assertThat(parent2).isInstanceOf(ChildResource.class);
        assertThat(parent2).isInstanceOf(DirectoryResource.class);

        final ChildResource child2 = (ChildResource) parent2;

        final ParentResource parent3 = child2.getParent();

        assertThat(dir.toString()).isEqualTo(parent3.getId());
        assertThat(parent3).isInstanceOf(ChildResource.class);
        assertThat(parent3).isInstanceOf(DirectoryResource.class);

        final ChildResource child3 = (ChildResource) parent3;

        final ParentResource parent4 = child3.getParent();

        assertThat(root.toString()).isEqualTo(parent4.getId());
    }

    private Configuration createConf(final String fsDefaultName) throws URISyntaxException {
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
