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

package uk.gov.gchq.palisade.service.audit.service;

import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

class AuditServiceTestCommon {

    static final long TEST_NUMBER_OF_RECORDS_PROCESSED = 20;
    static final long TEST_NUMBER_OF_RECORDS_RETURNED = 5;
    static final String TEST_TOKEN = "token in the form of a UUID";

    private static final String TEST_USER_ID = "an identifier for the user";
    private static final String TEST_RESOURCE_ID = "a pointer to a data resource";
    private static final String TEST_PURPOSE = "the purpose for the data access request";
    private static final String TEST_ORIGINAL_REQUEST_ID = "originalRequestId linking all logs from the same data access request together";
    private static final String TEST_DATA_TYPE = "data type of the resource, e.g. Employee";
    private static final String TEST_EXCEPTION_MESSAGE = "exception message";
    private static final String TEST_SERVICE_CLASS_MESSAGE = "mocked service class, e.g. UserService";

    private static final String TEST_RULES_APPLIED = "human readable description of the rules/policies been applied to the data";

    static UserId mockUserID() {
        final UserId mockUserId = Mockito.mock(UserId.class);
        Mockito.doReturn(TEST_USER_ID).when(mockUserId).getId();
        Mockito.doReturn(TEST_USER_ID).when(mockUserId).toString();
        return mockUserId;
    }

    static User mockUser() {
        final User mockUser = Mockito.mock(User.class);
        Mockito.doReturn(mockUserID()).when(mockUser).getUserId();
        Mockito.doReturn(TEST_USER_ID).when(mockUser).toString();
        return mockUser;
    }

    static Context mockContext() {
        final Context mockContext = Mockito.mock(Context.class);
        Mockito.doReturn(TEST_PURPOSE).when(mockContext).getPurpose();
        Mockito.doReturn(TEST_PURPOSE).when(mockContext).toString();
        return mockContext;
    }

    static RequestId mockOriginalRequestId() {
        final RequestId mockOriginalRequestId = Mockito.mock(RequestId.class);
        Mockito.doReturn(TEST_ORIGINAL_REQUEST_ID).when(mockOriginalRequestId).getId();
        Mockito.doReturn(TEST_ORIGINAL_REQUEST_ID).when(mockOriginalRequestId).toString();
        return mockOriginalRequestId;
    }

    static LeafResource mockResource() {
        final LeafResource mockResource = Mockito.mock(LeafResource.class);
        Mockito.doReturn(TEST_RESOURCE_ID).when(mockResource).getId();
        Mockito.doReturn(TEST_RESOURCE_ID).when(mockResource).toString();
        Mockito.doReturn(TEST_DATA_TYPE).when(mockResource).getType();
        Mockito.doReturn(TEST_DATA_TYPE).when(mockResource).toString();
        return mockResource;
    }

    static Exception mockException() {
        final Exception mockException = Mockito.mock(Exception.class);
        Mockito.doReturn(TEST_EXCEPTION_MESSAGE).when(mockException).getMessage();
        Mockito.doReturn(TEST_EXCEPTION_MESSAGE).when(mockException).toString();
        return mockException;
    }

    static Rules mockRules() {
        final Rules mockRules = Mockito.mock(Rules.class);
        Mockito.doReturn(TEST_RULES_APPLIED).when(mockRules).getMessage();
        Mockito.doReturn(TEST_RULES_APPLIED).when(mockRules).toString();
        return mockRules;
    }

    public static class UserService implements Service {
    }

    public static class ResourceService implements Service {
    }

    static AuditRequest.RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest() {
        return AuditRequest.RegisterRequestCompleteAuditRequest.create(mockOriginalRequestId())
                .withUser(mockUser())
                .withLeafResources(Sets.newSet(mockResource()))
                .withContext(mockContext());
    }

    static AuditRequest.RegisterRequestExceptionAuditRequest registerRequestExceptionAuditRequestFromUserService() {
        return AuditRequest.RegisterRequestExceptionAuditRequest.create(mockOriginalRequestId())
                .withUserId(mockUserID())
                .withResourceId(mockResource().getId())
                .withContext(mockContext())
                .withException(mockException())
                .withServiceName(ServiceName.USER_SERVICE.name());
    }

    static AuditRequest.ReadRequestCompleteAuditRequest readRequestCompleteAuditRequest() {
        return AuditRequest.ReadRequestCompleteAuditRequest.create(mockOriginalRequestId())
                .withUser(mockUser())
                .withLeafResource(mockResource())
                .withContext(mockContext())
                .withRulesApplied(mockRules())
                .withNumberOfRecordsReturned(TEST_NUMBER_OF_RECORDS_RETURNED)
                .withNumberOfRecordsProcessed(TEST_NUMBER_OF_RECORDS_PROCESSED);
    }

    static AuditRequest.ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest() {
        return AuditRequest.ReadRequestExceptionAuditRequest.create(mockOriginalRequestId())
                .withToken(TEST_TOKEN)
                .withLeafResource(mockResource())
                .withException(mockException());
    }
}
