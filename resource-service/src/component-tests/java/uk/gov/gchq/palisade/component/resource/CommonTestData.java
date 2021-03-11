/*
 * Copyright 2018-2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.palisade.component.resource;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;

public class CommonTestData {

    private CommonTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    private static final SimpleConnectionDetail DETAIL = new SimpleConnectionDetail().serviceName("data-service-mock");
    private static final SimpleConnectionDetail LOCALHOST_DETAIL = new SimpleConnectionDetail().serviceName("http://localhost:8082");
    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final User USER = new User().userId("test-user");
    public static final String EMPLOYEE_TYPE = "employee";
    public static final String CLIENT_TYPE = "client";
    public static final String AVRO_FORMAT = "avro";
    public static final String JSON_FORMAT = "json";
    public static final DirectoryResource TEST_DIRECTORY = (DirectoryResource) ResourceBuilder.create("file:/test/");
    public static final FileResource EMPLOYEE_AVRO_FILE = ((FileResource) ResourceBuilder.create("file:/test/employee.avro"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);
    public static final FileResource EMPLOYEE_JSON_FILE = ((FileResource) ResourceBuilder.create("file:/test/employee.json"))
            .type(EMPLOYEE_TYPE)
            .serialisedFormat(JSON_FORMAT)
            .connectionDetail(DETAIL);
    public static final FileResource CLIENT_AVRO_FILE = ((FileResource) ResourceBuilder.create("file:/test/client.avro"))
            .type(CLIENT_TYPE)
            .serialisedFormat(AVRO_FORMAT)
            .connectionDetail(DETAIL);

    public static final ResourceRequest TEST_DIRECTORY_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(TEST_DIRECTORY.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest EMPLOYEE_AVRO_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(EMPLOYEE_AVRO_FILE.getId())
            .withContext(CONTEXT)
            .withUser(USER);

    public static final String ROOT_PATH = System.getProperty("user.dir") + "/src/contract-tests/resources/root/";
    public static final DirectoryResource ROOT_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH).toURI());

    public static final DirectoryResource TOP_LEVEL_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/").toURI());
    public static final DirectoryResource EMPTY_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "empty-dir/").toURI());

    public static final DirectoryResource MULTI_FILE_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/").toURI());
    public static final DirectoryResource SINGLE_FILE_DIR = (DirectoryResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/single-file-dir/").toURI());

    public static final FileResource MULTI_FILE_ONE = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/multiFileOne.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(LOCALHOST_DETAIL);
    public static final FileResource MULTI_FILE_TWO = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/multi-file-dir/multiFileTwo.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(LOCALHOST_DETAIL);

    public static final FileResource SINGLE_FILE = ((FileResource) ResourceBuilder.create(new File(ROOT_PATH + "top-level-dir/single-file-dir/singleFile.txt").toURI()))
            .type("java.lang.String")
            .serialisedFormat("txt")
            .connectionDetail(LOCALHOST_DETAIL);

    public static final ResourceRequest MULTI_FILE_ONE_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(MULTI_FILE_ONE.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest MULTI_FILE_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(MULTI_FILE_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest TOP_LEVEL_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(TOP_LEVEL_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest EMPTY_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(EMPTY_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
    public static final ResourceRequest ROOT_DIR_REQUEST = ResourceRequest.Builder.create()
            .withUserId(USER.getUserId().getId())
            .withResourceId(ROOT_DIR.getId())
            .withContext(CONTEXT)
            .withUser(USER);
}
