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

package uk.gov.gchq.palisade.service.audit;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.HashMap;
import java.util.Map;

public final class ApplicationTestData {

    /**
     * Common test data for all classes
     */

    public static final String TEST_TOKEN = "token in the form of a UUID";

    public static final String TEST_USER_ID = "an identifier for the user";
    public static final String TEST_RESOURCE_ID = "a pointer to a data resource";
    public static final String TEST_PURPOSE = "the purpose for the data access request";
    public static final Context TEST_CONTEXT = new Context().purpose(TEST_PURPOSE);
    public static final String TEST_TIMESTAMP = "2020-02-20";
    public static final String TEST_SERVER_IP = "the IP address of the server";
    public static final String TEST_SERVER_NAME = "the name of the server";
    public static final Map<String, Object> TEST_ATTRIBUTES = new HashMap<>();
    public static final Throwable TEST_EXCEPTION = new Throwable("exception message");
    public static final String TEST_LEAF_RESOURCE_ID = "file:/test/resource/file.txt";

    static {
        TEST_ATTRIBUTES.put("test attribute key", "test attribute value");
    }

    public static AuditErrorMessage auditErrorMessage(final String serviceName) {
        return AuditErrorMessage.Builder.create()
                .withUserId(TEST_USER_ID)
                .withResourceId(TEST_RESOURCE_ID)
                .withContext(TEST_CONTEXT)
                .withServiceName(serviceName)
                .withTimestamp(TEST_TIMESTAMP)
                .withServerIp(TEST_SERVER_IP)
                .withServerHostname(TEST_SERVER_NAME)
                .withAttributes(TEST_ATTRIBUTES)
                .withError(TEST_EXCEPTION);
    }

    public static AuditSuccessMessage auditSuccessMessage(final String serviceName) {
        return AuditSuccessMessage.Builder.create()
                .withUserId(TEST_USER_ID)
                .withResourceId(TEST_RESOURCE_ID)
                .withContext(TEST_CONTEXT)
                .withServiceName(serviceName)
                .withTimestamp(TEST_TIMESTAMP)
                .withServerIp(TEST_SERVER_IP)
                .withServerHostname(TEST_SERVER_NAME)
                .withAttributes(TEST_ATTRIBUTES)
                .withLeafResourceId(TEST_LEAF_RESOURCE_ID);
    }
}
