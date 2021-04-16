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

import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.audit.service.ServiceName;

import java.util.HashMap;
import java.util.Map;

/**
 * Test data for Data Service tests
 */
public final class ApplicationTestData {

    /*
     * Common test data for all classes
     */

    /**
     * token in the form of a UUID
     */
    public static final String TEST_TOKEN = "token in the form of a UUID";

    /**
     * an identifier for the user
     */
    public static final String TEST_USER_ID = "an identifier for the user";

    /**
     * a pointer to a data resource
     */
    public static final String TEST_RESOURCE_ID = "a pointer to a data resource";

    /**
     * the purpose for the data access request
     */
    public static final String TEST_PURPOSE = "the purpose for the data access request";

    /**
     * test context
     */
    public static final Context TEST_CONTEXT = new Context().purpose(TEST_PURPOSE);

    /**
     * test timestamp
     */
    public static final String TEST_TIMESTAMP = "2020-02-20";

    /**
     * the IP address of the server
     */
    public static final String TEST_SERVER_IP = "the IP address of the server";

    /**
     * the name of the server
     */
    public static final String TEST_SERVER_NAME = "the name of the server";

    /**
     * test empty attribute map
     */
    public static final Map<String, Object> TEST_ATTRIBUTES = new HashMap<>();

    /**
     * test throwable
     */
    public static final Throwable TEST_EXCEPTION = new Throwable("exception message");

    /**
     * test leaf resource id
     */
    public static final String TEST_LEAF_RESOURCE_ID = "file:/test/resource/file.txt";

    static {
        TEST_ATTRIBUTES.put("test attribute key", "test attribute value");
    }

    /**
     * Returns a newly created {@code AuditErrorMessage}
     *
     * @param serviceName the service name
     * @return a newly created {@code AuditErrorMessage}
     */
    public static AuditErrorMessage auditErrorMessage(final ServiceName serviceName) {
        return auditErrorMessage(serviceName.value);
    }

    /**
     * Returns a newly created {@code AuditSuccessMessage}
     *
     * @param serviceName the service name
     * @return a newly created {@code AuditSuccessMessage}
     */
    public static AuditSuccessMessage auditSuccessMessage(final ServiceName serviceName) {
        return auditSuccessMessage(serviceName.value);
    }

    /**
     * Returns a newly created {@code AuditErrorMessage}
     *
     * @param serviceName the service name
     * @return a newly created {@code AuditErrorMessage}
     */
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

    /**
     * Returns a newly created {@code AuditSuccessMessage}
     *
     * @param serviceName the service name
     * @return a newly created {@code AuditSuccessMessage}
     */
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
