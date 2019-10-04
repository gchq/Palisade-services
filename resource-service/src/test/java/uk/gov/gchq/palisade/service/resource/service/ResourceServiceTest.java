/*
 * Copyright 2019 Crown Copyright
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

import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.resource.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByIdRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class ResourceServiceTest {

    private static final String FORMAT_VALUE = "txt";
    private static final String TYPE_VALUE = "bob";
    private static final String FILE_NAME_VALUE_00001 = "00001";
    private static final String FILE_NAME_VALUE_00002 = "00002";
    private static final String FILE = System.getProperty("os.name").toLowerCase().startsWith("win") ? "file:///" : "file://";
    public static final String HDFS = "hdfs:///";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceTest.class);
    private static File TMP_DIRECTORY;
    private SimpleConnectionDetail simpleConnection;
    private Configuration conf;
    private String inputPathString;
    private FileSystem fs;
    private HashMap<Resource, ConnectionDetail> expected;
    private SimpleCacheService simpleCache;
    private ResourceService resourceService;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder(TMP_DIRECTORY);

    @Before
    public void setup() throws Exception {
        System.setProperty("hadoop.home.dir", Paths.get(".").toAbsolutePath().normalize().toString() + "/src/test/resources/hadoop-3.2.0");
        conf = createConf();
        inputPathString = testFolder.getRoot().getAbsolutePath() + "/inputDir";
        fs = FileSystem.get(conf);
        fs.mkdirs(new Path(inputPathString));
        expected = Maps.newHashMap();
        simpleConnection = new SimpleConnectionDetail();

        simpleCache = new SimpleCacheService().backingStore(new HashMapBackingStore(true));

        resourceService = new ResourceService(conf, simpleCache);
        resourceService.addDataService(simpleConnection);
    }

    @Test
    public void getMapCompletableFutureTest() throws Exception {
        //given
        final String id = inputPathString.replace("\\", "/") + "/" + getFileNameFromResourceDetails(FILE_NAME_VALUE_00001, TYPE_VALUE, FORMAT_VALUE);
        writeFile(fs, inputPathString, FILE_NAME_VALUE_00001, FORMAT_VALUE, TYPE_VALUE);
        writeFile(fs, inputPathString, FILE_NAME_VALUE_00002, FORMAT_VALUE, TYPE_VALUE);
        expected.put(new FileResource().id(FILE + id).type(TYPE_VALUE).serialisedFormat(FORMAT_VALUE).parent(
                new DirectoryResource().id(FILE + inputPathString.replace("\\", "/")).parent(
                        new SystemResource().id(FILE + testFolder.getRoot().getAbsolutePath())
                )
        ), simpleConnection);

        //when
        final CompletableFuture<Map<LeafResource, ConnectionDetail>> resourcesById = resourceService.getResourcesById(new GetResourcesByIdRequest().resourceId(FILE + id));

        //then
        assertEquals(expected, resourcesById.join());
    }

    @Test
    public void addResourceTest() throws Exception {
        try {
            resourceService.addResource(null);
            fail("exception expected");
        } catch (UnsupportedOperationException e) {
            assertEquals(ResourceService.ERROR_ADD_RESOURCE, e.getMessage());
        }
    }

    @Test
    public void shouldResolveParentsTest() {
        final String parent = testFolder.getRoot().getAbsolutePath().replace("\\", "/") + "/inputDir" + "/" + "folder1" + "/" + "folder2" + "/";
        final String id = parent + getFileNameFromResourceDetails(FILE_NAME_VALUE_00001, TYPE_VALUE, FORMAT_VALUE);

        final FileResource fileResource = new FileResource().id(id);
        ResourceService.resolveParents(fileResource, conf);

        final ParentResource parent1 = fileResource.getParent();
        assertEquals(parent, parent1.getId());

        assertTrue(parent1 instanceof ChildResource);
        assertTrue(parent1 instanceof DirectoryResource);

        final ChildResource child = (ChildResource) parent1;
        ResourceService.resolveParents(child, conf);

        final ParentResource parent2 = child.getParent();
        assertEquals(testFolder.getRoot().getAbsolutePath().replace("\\", "/") + "/inputDir" + "/" + "folder1" + "/", parent2.getId());

        assertTrue(parent2 instanceof ChildResource);
        assertTrue(parent2 instanceof DirectoryResource);

        final ChildResource child2 = (ChildResource) parent2;
        ResourceService.resolveParents(child2, conf);

        final ParentResource parent3 = child2.getParent();
        assertEquals(testFolder.getRoot().getAbsolutePath().replace("\\", "/") + "/inputDir" + "/", parent3.getId());

        assertTrue(parent3 instanceof ChildResource);
        assertTrue(parent3 instanceof DirectoryResource);

        final ChildResource child3 = (ChildResource) parent3;
        ResourceService.resolveParents(child3, conf);

        final ParentResource parent4 = child3.getParent();
        assertEquals(testFolder.getRoot().getAbsolutePath().replace("\\", "/") + "/", parent4.getId());

        assertTrue(parent4 instanceof SystemResource);
        assertFalse(parent4 instanceof DirectoryResource);
    }

    private void writeFile(final FileSystem fs, final String parentPath, final String name, final String format, final String type) throws IOException {
        writeFile(fs, parentPath + "/" + getFileNameFromResourceDetails(name, type, format));
    }

    private void writeFile(final FileSystem fs, final String filePathString) throws IOException {
        //Write Some file
        final Path filePath = new Path(filePathString);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(filePath, true)))) {
            writer.write("myContents");
        }
    }

    private Configuration createConf() {
        // Set up local conf
        final Configuration conf = new Configuration();
        conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, FILE + testFolder.getRoot().getAbsolutePath().replace("\\", "/"));
        return conf;
    }

    private static String getFileNameFromResourceDetails(final String name, final String type, final String format) {
        //Type, Id, Format
        return String.format(ResourceDetails.FILE_NAME_FORMAT, type, name, format);
    }
}
