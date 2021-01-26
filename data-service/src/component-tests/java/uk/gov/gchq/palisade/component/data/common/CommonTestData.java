/*
 * Copyright 2021 Crown Copyright
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
package uk.gov.gchq.palisade.component.data.common;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataReaderResponse;
import uk.gov.gchq.palisade.service.data.model.DataReaderRequestModel;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;

import java.util.Collections;
import java.util.Map;

/**
 * Set of constants that are used in the testing
 */
public class CommonTestData {

    private CommonTestData() {
    }

    public static final String TOKEN = "test-request-token";

    public static final String LEAF_RESOURCE_ID = "test leaf resource id";

    public static final DataRequestModel DATA_REQUEST_MODEL = DataRequestModel.Builder.create()
            .withToken(TOKEN)
            .withLeafResourceId(LEAF_RESOURCE_ID);

    public static final Context CONTEXT = new Context().purpose("testContext");

    public static final String USER_ID = "testUserId";

    public static final User USER = new User().userId(USER_ID);

    public static final String RESOURCE_ID = "test resource id";

    public static final LeafResource RESOURCE = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));

    public static final Rules<LeafResource> RULES = new Rules<>();

    public static final Map<String, Object> ATTRIBUTES = Collections.singletonMap("test key", "test value");

    public static final AuditSuccessMessage AUDIT_SUCCESS_MESSAGE = AuditSuccessMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withToken(TOKEN)
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes(ATTRIBUTES);

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withToken(TOKEN)
            .withUserId(null)
            .withResourceId(null)
            .withContext(null)
            .withAttributes(null)
            .withError(new ForbiddenException("Something went wrong!"));


    public static final DataReaderRequestModel DATA_READER_REQUEST_MODEL = DataReaderRequestModel.Builder.create()
            .withResource(RESOURCE)
            .withUser(USER)
            .withContext(CONTEXT)
            .withRules(RULES);

    public static final AuditableDataReaderRequest AUDITABLE_DATA_READER_REQUEST = AuditableDataReaderRequest.Builder.create()
            .withDataRequestModel(DATA_REQUEST_MODEL)
            .withDataReaderRequestModel(DATA_READER_REQUEST_MODEL)
            .withErrorMessage(null);


    public static final AuditableDataReaderRequest AUDITABLE_DATA_READER_REQUEST_WITH_ERROR = AuditableDataReaderRequest.Builder.create()
            .withDataRequestModel(DATA_REQUEST_MODEL)
            .withDataReaderRequestModel(null)
            .withErrorMessage(AUDIT_ERROR_MESSAGE);


    public static final AuditableDataReaderResponse AUDITABLE_DATA_READER_RESPONSE = AuditableDataReaderResponse.Builder.create()
            .withToken(TOKEN)
            .withSuccessMessage(AUDIT_SUCCESS_MESSAGE)
            .withAuditErrorMessage(null);

    public static final AuditableDataReaderResponse AUDITABLE_DATA_READER_RESPONSE_WITH_ERROR = AuditableDataReaderResponse.Builder.create()
            .withToken(TOKEN)
            .withSuccessMessage(null)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);

    public static final TokenMessagePair TOKEN_MESSAGE_PAIR = new TokenMessagePair(TOKEN, AUDIT_SUCCESS_MESSAGE);
}
