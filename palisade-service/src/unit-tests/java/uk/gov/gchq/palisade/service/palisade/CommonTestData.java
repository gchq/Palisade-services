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

package uk.gov.gchq.palisade.service.palisade;

import uk.gov.gchq.palisade.service.palisade.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.palisade.model.AuditablePalisadeSystemResponse;
import uk.gov.gchq.palisade.service.palisade.model.PalisadeClientRequest;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CommonTestData {
    public static final Throwable ERROR = new Throwable("An error was thrown in the Palisade-Service");
    public static final PalisadeClientRequest PALISADE_REQUEST;
    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE;
    public static final AuditablePalisadeSystemResponse AUDITABLE_PALISADE_REQUEST;
    public static final AuditablePalisadeSystemResponse AUDITABLE_PALISADE_ERROR;
    public static final UUID COMMON_UUID = java.util.UUID.fromString("df3fc6ef-3f8c-48b4-ae1b-5f3d8ad32ead");

    static {
        PALISADE_REQUEST = PalisadeClientRequest.Builder.create()
                .withUserId("testUserId")
                .withResourceId("/test/resourceId")
                .withContext(Collections.singletonMap("purpose", "testContext"));

        AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder
                .create(PALISADE_REQUEST, Map.of("messages", "10")).withError(ERROR);

        AUDITABLE_PALISADE_REQUEST = AuditablePalisadeSystemResponse.Builder
                .create().withPalisadeRequest(PALISADE_REQUEST);

        AUDITABLE_PALISADE_ERROR = AuditablePalisadeSystemResponse.Builder
                .create().withAuditErrorMessage(AUDIT_ERROR_MESSAGE);
    }
}
