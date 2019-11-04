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
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestExceptionAuditRequest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LoggerAuditServiceTest extends AuditServiceTestCommon {

    @Mock
    Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    @Captor
    ArgumentCaptor<Object> logCaptor;

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
        final AuditRequest auditRequest = RegisterRequestCompleteAuditRequest.create(requestId)
                .withUser(user)
                .withLeafResources(Sets.newSet(resource))
                .withContext(context);

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).info(logCaptor.capture().toString());
        final String log = logCaptor.getValue().toString();

        assertThat(log, allOf(
                containsString(user.toString()),
                containsString(context.toString()),
                containsString(requestId.toString())
        ));
        assertThat(log, allOf(
                containsString(LoggerAuditService.REGISTER_REQUEST_COMPLETE)
        ));
    }

    @Test
    public void auditRegisterRequestException() {
        // Given
        final AuditRequest auditRequest = RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceClass(Service.class);

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).error(logCaptor.capture().toString());
        final String log = logCaptor.getValue().toString();

        assertThat(log, allOf(
                containsString(userId.toString()),
                containsString(context.toString()),
                containsString(requestId.toString()),
                containsString(resource.getId()),
                containsString(exception.toString())
        ));
        assertThat(log, allOf(
                containsString(LoggerAuditService.REGISTER_REQUEST_EXCEPTION)
        ));
    }

    @Test
    public void auditReadRequestSuccessful() {
        // Given
        final AuditRequest auditRequest = ReadRequestCompleteAuditRequest.create(requestId)
                .withUser(user)
                .withLeafResource(resource)
                .withContext(context)
                .withRulesApplied(rules)
                .withNumberOfRecordsReturned(TEST_NUMBER_OF_RECORDS_RETURNED)
                .withNumberOfRecordsProcessed(TEST_NUMBER_OF_RECORDS_PROCESSED);

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).info(logCaptor.capture().toString());
        final String log = logCaptor.getValue().toString();

        assertThat(log, allOf(
                containsString(user.toString()),
                containsString(context.toString()),
                containsString(rules.toString()),
                containsString(resource.toString())
        ));
        assertThat(log, allOf(
                containsString(LoggerAuditService.READ_REQUEST_COMPLETE),
                containsString(String.valueOf(TEST_NUMBER_OF_RECORDS_RETURNED)),
                containsString(String.valueOf(TEST_NUMBER_OF_RECORDS_PROCESSED))
        ));
    }

    @Test
    public void auditReadRequestException() {

        // Given
        final AuditRequest auditRequest = ReadRequestExceptionAuditRequest.create(requestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(resource)
                .withException(exception);

        // When
        auditService.audit(auditRequest);

        // Then
        verify(logger, atLeastOnce()).error(logCaptor.capture().toString());
        final String log = logCaptor.getValue().toString();

        assertThat(log, allOf(
                containsString(requestId.toString()),
                containsString(resource.toString()),
                containsString(resource.toString()),
                containsString(exception.toString())
        ));
        assertThat(log, allOf(
                containsString(LoggerAuditService.READ_REQUEST_EXCEPTION)
        ));
    }

}
