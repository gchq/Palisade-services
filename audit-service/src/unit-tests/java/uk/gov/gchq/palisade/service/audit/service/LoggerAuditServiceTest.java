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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LoggerAuditServiceTest extends AuditServiceTestCommon {

    @Mock
    Logger logger;
    @Captor
    ArgumentCaptor<String> logCaptor;

    private static LoggerAuditService auditService;
    private UserId userId;
    private User user;
    private Context context;
    private RequestId requestId;
    private LeafResource resource;
    private Exception exception;
    private Rules rules;

    @Before
    public void setUp() {
        auditService = new LoggerAuditService(logger);

        userId = mockUserID();
        user = mockUser();
        context = mockContext();
        requestId = mockOriginalRequestId();
        resource = mockResource();
        exception = mockException();
        rules = mockRules();
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

        // Then
        verify(logger, atLeastOnce()).info(logCaptor.capture());
        final String log = logCaptor.getValue();

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(user.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(context.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(requestId.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(LoggerAuditService.REGISTER_REQUEST_COMPLETE))
        );
    }

    @Test
    public void auditRegisterRequestException() {
        // Given
        final AuditRequest auditRequest = AuditRequest.RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceName(ServiceName.USER_SERVICE.name());

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).error(logCaptor.capture());
        final String log = logCaptor.getValue();

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(userId.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(context.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(requestId.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(resource.getId())),
                () -> assertThat(logCaptor.getAllValues().contains(exception.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(LoggerAuditService.REGISTER_REQUEST_EXCEPTION))
        );
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

        // Then
        verify(logger, atLeastOnce()).info(logCaptor.capture());
        final String log = logCaptor.getValue();

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(user.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(context.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(rules.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(resource.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(LoggerAuditService.READ_REQUEST_COMPLETE)),
                () -> assertThat(logCaptor.getAllValues().contains(String.valueOf(TEST_NUMBER_OF_RECORDS_RETURNED))),
                () -> assertThat(logCaptor.getAllValues().contains(String.valueOf(TEST_NUMBER_OF_RECORDS_PROCESSED)))
        );
    }

    @Test
    public void auditReadRequestException() {

        // Given
        final AuditRequest auditRequest = AuditRequest.ReadRequestExceptionAuditRequest.create(requestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(resource)
                .withException(exception);

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).error(logCaptor.capture());
        final String log = logCaptor.getValue();

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(requestId.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(resource.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(exception.toString())),
                () -> assertThat(logCaptor.getAllValues().contains(LoggerAuditService.READ_REQUEST_EXCEPTION))
        );
    }
}
