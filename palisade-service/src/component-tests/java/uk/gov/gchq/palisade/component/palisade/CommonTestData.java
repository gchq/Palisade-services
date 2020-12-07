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

package uk.gov.gchq.palisade.component.palisade;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.AuditablePalisadeRequest;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeRequest;

import java.util.HashMap;
import java.util.UUID;

/**
 * Common test data for all classes
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class CommonTestData {

    public static final String USER_ID = "testUserId";
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String CONTEXT_PURPOSE = "testContext";
    public static final Context CONTEXT = new Context().purpose(CONTEXT_PURPOSE);
    public static final UUID COMMON_UUID = UUID.fromString("df3fc6ef-3f8c-48b4-ae1b-5f3d8ad32ead");
    public static final String KEY_NULL_VALUE = "$.keyToNull";
    public static final Throwable ERROR = new Throwable("An error");

    public static final PalisadeRequest PALISADE_REQUEST = PalisadeRequest.Builder.create()
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT);

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create(PALISADE_REQUEST, new HashMap<>()).withError(ERROR);

    public static final AuditablePalisadeRequest AUDITABLE_WITH_REQUEST = AuditablePalisadeRequest.Builder.create()
            .withPalisadeRequest(PALISADE_REQUEST);
    public static final AuditablePalisadeRequest AUDITABLE_WITH_ERROR = AuditablePalisadeRequest.Builder.create()
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);
}
