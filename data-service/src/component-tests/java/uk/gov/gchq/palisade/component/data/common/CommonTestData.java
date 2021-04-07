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
package uk.gov.gchq.palisade.component.data.common;

import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.User;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.reader.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.exception.ReadException;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

import java.util.Collections;
import java.util.Map;

/**
 * Set of constants that are used in the testing
 */
public class CommonTestData {

    private CommonTestData() {
    }

    public static final String TOKEN = "test-request-token";
    public static final String LEAF_RESOURCE_ID = "file:/test/resource/file.txt";
    public static final DataRequest DATA_REQUEST = DataRequest.Builder.create()
            .withToken(TOKEN)
            .withLeafResourceId(LEAF_RESOURCE_ID);

    public static final Context CONTEXT = new Context().purpose("testContext");
    public static final String USER_ID = "testUserId";
    public static final String RESOURCE_ID = "test resource id";

    public static final LeafResource RESOURCE = new FileResource().id(LEAF_RESOURCE_ID)
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));

    public static final Rules<LeafResource> RULES = new Rules<>();
    public static final Map<String, Object> ATTRIBUTES = Collections.singletonMap("test key", "test value");

    public static final AuditSuccessMessage AUDIT_SUCCESS_MESSAGE = AuditSuccessMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes(ATTRIBUTES);

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes(ATTRIBUTES)
            .withError(new ReadException("Reading problems!"));

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE_FAILED_AUTHENTICATION = AuditErrorMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withUserId(null)
            .withResourceId(null)
            .withContext(null)
            .withAttributes(null)
            .withError(new ForbiddenException("Something went wrong!"));

    public static final AuthorisedDataRequest AUTHORISED_DATA = AuthorisedDataRequest.Builder.create()
            .withResource(RESOURCE)
            .withUser(new User().userId(USER_ID))
            .withContext(CONTEXT)
            .withRules(RULES);

    public static final AuditableAuthorisedDataRequest AUDITABLE_DATA_REQUEST = AuditableAuthorisedDataRequest.Builder.create()
            .withDataRequest(DATA_REQUEST)
            .withAuthorisedData(AUTHORISED_DATA);

    public static final AuditableAuthorisedDataRequest AUDITABLE_DATA_REQUEST_WITH_ERROR = AuditableAuthorisedDataRequest.Builder.create()
            .withDataRequest(DATA_REQUEST)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);

    public static final AuditableDataResponse AUDITABLE_DATA_RESPONSE = AuditableDataResponse.Builder.create()
            .withToken(TOKEN)
            .withSuccessMessage(AUDIT_SUCCESS_MESSAGE)
            .withAuditErrorMessage(null);

    public static final AuthorisedRequestEntity ENTITY1 = new AuthorisedRequestEntity(
            TOKEN + "1",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "1"),
            new Context(),
            new Rules<>()
    );

    public static final AuthorisedRequestEntity ENTITY2 = new AuthorisedRequestEntity(
            TOKEN + "2",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "1"),
            new Context(),
            new Rules<>()
    );

    public static final AuthorisedRequestEntity ENTITY3 = new AuthorisedRequestEntity(
            TOKEN + "1",
            new User().userId("user-id"),
            new FileResource().id(RESOURCE_ID + "3"),
            new Context(),
            new Rules<>()
    );

}
