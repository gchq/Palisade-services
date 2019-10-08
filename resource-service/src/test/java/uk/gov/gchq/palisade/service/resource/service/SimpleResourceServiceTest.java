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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.resource.repository.SimpleCacheService;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class SimpleResourceServiceTest {

    private static final String FORMAT_VALUE = "txt";
    private static final String TYPE_VALUE = "bob";
    private static final String FILE_NAME_VALUE_00001 = "00001";
    private static final String FILE_NAME_VALUE_00002 = "00002";
    private static final String FILE = System.getProperty("os.name").toLowerCase().startsWith("win") ? "file:///" : "file://";
    public static final String HDFS = "hdfs:///";

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceServiceTest.class);
    private static File TMP_DIRECTORY;
    private SimpleConnectionDetail simpleConnection;
    private String inputPathString;
    private HashMap<Resource, ConnectionDetail> expected;
    private SimpleCacheService simpleCache;
    private SimpleResourceService resourceService;
    private ApplicationConfiguration config = new ApplicationConfiguration();
    private ResourceService service;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder(TMP_DIRECTORY);

    @Before
    public void setup() throws Exception {
        inputPathString = testFolder.getRoot().getAbsolutePath() + "/inputDir";
        expected = Maps.newHashMap();
        simpleConnection = new SimpleConnectionDetail();

        simpleCache = new SimpleCacheService().backingStore(new HashMapBackingStore(true));

        resourceService = new SimpleResourceService(service, config.getAsyncExecutor());
    }

    @Test
    public void createResourceServiceTest() {
        LOGGER.debug("Resource Service {}", resourceService);
    }

    @Test
    public void addResourceTest() throws Exception {
        try {
            resourceService.addResource(null);
            fail("exception expected");
        } catch (UnsupportedOperationException e) {
            assertEquals(SimpleResourceService.ERROR_ADD_RESOURCE, e.getMessage());
        }
    }

    private static String getFileNameFromResourceDetails(final String name, final String type, final String format) {
        //Type, Id, Format
        return String.format(ResourceDetails.FILE_NAME_FORMAT, type, name, format);
    }
}
