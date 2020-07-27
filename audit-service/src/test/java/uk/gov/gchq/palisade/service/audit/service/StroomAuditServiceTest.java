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

package uk.gov.gchq.palisade.service.audit.service;

import event.logging.Event;
import event.logging.impl.DefaultEventLoggingService;
import event.logging.impl.DefaultEventSerializer;
import event.logging.impl.EventSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class StroomAuditServiceTest extends AuditServiceTestCommon {
    private static final String TOKEN_NOT_FOUND_MESSAGE = "User's request was not in the cache: ";

    @Spy
    DefaultEventLoggingService eventLogger = new DefaultEventLoggingService();
    @Captor
    ArgumentCaptor<Event> logCaptor;
    private static EventSerializer eventSerializer = new DefaultEventSerializer();

    private static StroomAuditService auditService;
    private UserId userId;
    private User user;
    private Context context;
    private RequestId requestId;
    private LeafResource resource;
    private Exception exception;
    private Rules rules;

    @Before
    public void setUp() {
        auditService = new StroomAuditService(eventLogger)
                .organisation("Test Org")
                .systemClassification("Some system classification")
                .systemDescription("some system description")
                .systemEnv("some system env")
                .systemName("some system name")
                .systemVersion("some system version");
        userId = mockUserID();
        user = mockUser();
        context = mockContext();
        requestId = mockOriginalRequestId();
        resource = mockResource();
        exception = mockException();
        rules = mockRules();
    }

    @Test
    public void auditRegisterRequestWithNoResources() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestCompleteAuditRequest.create(requestId)
                .withUser(user)
                .withLeafResources(new HashSet<>(0))
                .withContext(context);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(userId.getId()),
                containsString(context.getPurpose()),
                containsString(requestId.getId())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_TYPE_ID),
                containsString(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_DESCRIPTION),
                containsString(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION)
        ));
    }

    @Test
    public void auditRegisterRequestSuccessful() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestCompleteAuditRequest.create(requestId)
                .withUser(user)
                .withLeafResources(Sets.newSet(resource))
                .withContext(context);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(userId.getId()),
                containsString(context.getPurpose()),
                containsString(requestId.getId()),
                containsString(resource.getId()),
                containsString(resource.getType())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.REGISTER_REQUEST_COMPLETED_TYPE_ID),
                containsString(StroomAuditService.REGISTER_REQUEST_COMPLETED_DESCRIPTION)
        ));
    }

    @Test
    public void auditRegisterRequestUserException() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceName(ServiceName.USER_SERVICE.name());
        auditRequest.setOriginalRequestId(requestId);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(userId.getId()),
                containsString(context.getPurpose()),
                containsString(requestId.getId()),
                containsString(resource.getId())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_TYPE_ID),
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_DESCRIPTION),
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_OUTCOME_DESCRIPTION)
        ));
    }

    @Test
    public void auditRegisterRequestResourceException() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceName(ServiceName.RESOURCE_SERVICE.name());

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(userId.getId()),
                containsString(context.getPurpose()),
                containsString(requestId.getId()),
                containsString(resource.getId())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_TYPE_ID),
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_DESCRIPTION),
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_OUTCOME_DESCRIPTION)
        ));
    }

    @Test
    public void auditRegisterRequestOtherException() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceName(ServiceName.TEST_SERVICE.name());

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        // better error messages
        assertThat(log)
            .contains(userId.getId())
            .contains(context.getPurpose())
            .contains(requestId.getId())
            .contains(resource.getId())
            .contains(exception.getMessage());

        assertThat(log, allOf(
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID),
                containsString(StroomAuditService.REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION)
        ));
    }

    @Test
    public void auditReadRequestSuccessful() {
        // Given
        final AuditRequest auditRequest = AuditRequest.ReadRequestCompleteAuditRequest.create(requestId)
                .withUser(user)
                .withLeafResource(resource)
                .withContext(context)
                .withRulesApplied(rules)
                .withNumberOfRecordsReturned(TEST_NUMBER_OF_RECORDS_RETURNED)
                .withNumberOfRecordsProcessed(TEST_NUMBER_OF_RECORDS_PROCESSED);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(userId.getId()),
                containsString(context.getPurpose()),
                containsString(requestId.getId()),
                containsString(resource.getId()),
                containsString(resource.getType()),
                containsString(rules.getMessage())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.READ_REQUEST_COMPLETED_TYPE_ID),
                containsString(StroomAuditService.READ_REQUEST_COMPLETED_DESCRIPTION)
        ));
    }

    @Test
    public void auditReadRequestTokenException() {
        // Given
        Mockito.doReturn(TOKEN_NOT_FOUND_MESSAGE).when(exception).getMessage();
        final AuditRequest auditRequest = AuditRequest.ReadRequestExceptionAuditRequest.create(requestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(resource)
                .withException(exception);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(requestId.getId()),
                containsString(resource.getId()),
                containsString(resource.getType())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_TYPE_ID),
                containsString(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_DESCRIPTION),
                containsString(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_OUTCOME_DESCRIPTION)
        ));
    }

    @Test
    public void auditReadRequestOtherException() {
        // Given
        final AuditRequest auditRequest = AuditRequest.ReadRequestExceptionAuditRequest.create(requestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(resource)
                .withException(exception);

        // When
        auditService.audit(auditRequest);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertThat(log, allOf(
                containsString(requestId.getId()),
                containsString(resource.getId()),
                containsString(resource.getType()),
                containsString(exception.getMessage())
        ));
        assertThat(log, allOf(
                containsString(StroomAuditService.READ_REQUEST_EXCEPTION_OTHER_TYPE_ID),
                containsString(StroomAuditService.READ_REQUEST_EXCEPTION_OTHER_DESCRIPTION)
        ));
    }
}
