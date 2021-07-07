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

package uk.gov.gchq.palisade.component.audit;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.Date;
import java.util.HashMap;

/**
 * Common test data for all classes
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class CommonTestData {

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final String USER_ID = "testUserId";
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final Context CONTEXT = new Context().purpose("testContext");

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withServiceName("testService")
            .withTimestamp(new Date().toString())
            .withServerIp("127.0.0.1")
            .withServerHostname("localhost")
            .withAttributes(new HashMap<>())
            .withError(new Throwable("Test error"));

    public static final AuditSuccessMessage AUDIT_SUCCESS_MESSAGE = AuditSuccessMessage.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withServiceName("testService")
            .withTimestamp(new Date().toString())
            .withServerIp("127.0.0.1")
            .withServerHostname("localhost")
            .withAttributes(new HashMap<>())
            .withLeafResourceId(RESOURCE_ID);
}
