package uk.gov.gchq.palisade.service.audit.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.request.AuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.ReadRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.audit.request.RegisterRequestExceptionAuditRequest;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.palisade.service.UserService;

import java.util.HashSet;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StroomAuditServiceTest {

    private static final StroomAuditService STROOM_AUDIT_SERVICE = createStroomAuditService();
    private static final String TEST_USER_ID = "an identifier for the user";
    private static final String TEST_RESOURCE_ID = "a pointer to a data resource";
    private static final String TEST_PURPOSE = "the purpose for the data access request";
    private static final String TEST_ORIGINAL_REQUEST_ID = "originalRequestId linking all logs from the same data access request together";
    private static final String TEST_SERIALISED_FORMAT = "serialised format of the resource, e.g. Avro, CSV";
    private static final String TEST_DATA_TYPE = "data type of the resource, e.g. Employee";
    private static final String TEST_EXCEPTION_MESSAGE = "exception message";
    private static final long TEST_NUMBER_OF_RECORDS_PROCESSED = 20;
    private static final long TEST_NUMBER_OF_RECORDS_RETURNED = 5;
    private static final String TEST_TOKEN = "token in the form of a UUID";
    private static final String TEST_RULES_APPLIED = "human readable description of the rules/policies been applied to the data";

    private static StroomAuditService createStroomAuditService() {
        return new StroomAuditService()
                .organisation("Test Org")
                .systemClassification("Some system classification")
                .systemDescription("some system description")
                .systemEnv("some system env")
                .systemName("some system name")
                .systemVersion("some system version");
    }

    private UserId mockUserID() {
        final UserId mockUserId = Mockito.mock(UserId.class);
        Mockito.doReturn(TEST_USER_ID).when(mockUserId).getId();
        return mockUserId;
    }

    private User mockUser() {
        final User mockUser = Mockito.mock(User.class);
        Mockito.doReturn(mockUserID()).when(mockUser).getUserId();
        return mockUser;
    }

    private Context mockContext() {
        final Context mockContext = Mockito.mock(Context.class);
        Mockito.doReturn(TEST_PURPOSE).when(mockContext).getPurpose();
        return mockContext;
    }

    private RequestId mockOriginalRequestId() {
        final RequestId mockOriginalRequestId = Mockito.mock(RequestId.class);
        Mockito.doReturn(TEST_ORIGINAL_REQUEST_ID).when(mockOriginalRequestId).getId();
        return mockOriginalRequestId;
    }

    private LeafResource mockResource() {
        final LeafResource mockResource = Mockito.mock(LeafResource.class);
        Mockito.doReturn(TEST_RESOURCE_ID).when(mockResource).getId();
        Mockito.doReturn(TEST_DATA_TYPE).when(mockResource).getType();
        return mockResource;
    }

    private Exception mockException() {
        final Exception mockException = Mockito.mock(Exception.class);
        Mockito.doReturn(TEST_EXCEPTION_MESSAGE).when(mockException).getMessage();
        return mockException;
    }

    private Rules mockRules() {
        final Rules mockRules = Mockito.mock(Rules.class);
        Mockito.doReturn(TEST_RULES_APPLIED).when(mockRules).getMessage();
        return mockRules;
    }


    @Mock
    Appender appender;
    @Captor
    ArgumentCaptor<ILoggingEvent> logCaptor;

    @Test
    public void auditRegisterRequestWithNoResources() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback Error");
        }

        // mock user
        final User mockUser = mockUser();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();

        final AuditRequest auditRequest = RegisterRequestCompleteAuditRequest.create(mockOriginalRequestId)
                .withUser(mockUser)
                .withLeafResources(new HashSet<>(0))
                .withContext(mockContext);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUser, Mockito.atLeastOnce()).getUserId();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_DESCRIPTION));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION));
    }

    @Test
    public void auditRegisterRequestSuccessful() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }
        // mock user
        final User mockUser = mockUser();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock resource
        final LeafResource mockResource = mockResource();

        final AuditRequest auditRequest = RegisterRequestCompleteAuditRequest.create(mockOriginalRequestId)
                .withUser(mockUser)
                .withLeafResources(Sets.newSet(mockResource))
                .withContext(mockContext);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUser, Mockito.atLeastOnce()).getUserId();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        verify(mockResource, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getType();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(TEST_DATA_TYPE));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_COMPLETED_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_COMPLETED_DESCRIPTION));
    }

    @Test
    public void auditRegisterRequestUserException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }
        // mock userId
        final UserId mockUserId = mockUserID();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock exception
        final Exception mockException = mockException();

        final AuditRequest auditRequest = RegisterRequestExceptionAuditRequest.create(mockOriginalRequestId)
                .withUserId(mockUserId)
                .withResourceId(TEST_RESOURCE_ID)
                .withContext(mockContext)
                .withException(mockException)
                .withServiceClass(UserService.class);
        auditRequest.setOriginalRequestId(mockOriginalRequestId);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUserId, Mockito.atLeastOnce()).getId();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_DESCRIPTION));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_USER_OUTCOME_DESCRIPTION));
    }

    @Test
    public void auditRegisterRequestResourceException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }
        // mock userId
        final UserId mockUserId = mockUserID();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock exception
        final Exception mockException = mockException();

        final AuditRequest auditRequest = RegisterRequestExceptionAuditRequest.create(mockOriginalRequestId)
                .withUserId(mockUserId)
                .withResourceId(TEST_RESOURCE_ID)
                .withContext(mockContext)
                .withException(mockException)
                .withServiceClass(ResourceService.class);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUserId, Mockito.atLeastOnce()).getId();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_DESCRIPTION));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_RESOURCE_OUTCOME_DESCRIPTION));
    }

    @Test
    public void auditRegisterRequestOtherException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }

        // mock userId
        final UserId mockUserId = mockUserID();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock exception
        final Exception mockException = mockException();

        final AuditRequest auditRequest = RegisterRequestExceptionAuditRequest.create(mockOriginalRequestId)
                .withUserId(mockUserId)
                .withResourceId(TEST_RESOURCE_ID)
                .withContext(mockContext)
                .withException(mockException)
                .withServiceClass(AuditService.class);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUserId, Mockito.atLeastOnce()).getId();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        verify(mockException, Mockito.atLeastOnce()).getMessage();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(TEST_EXCEPTION_MESSAGE));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_OTHER_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.REGISTER_REQUEST_EXCEPTION_OTHER_DESCRIPTION));
    }

    @Test
    public void auditReadRequestSuccessful() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }

        // mock user
        final User mockUser = mockUser();
        // mock context
        final Context mockContext = mockContext();
        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock resource
        final LeafResource mockResource = mockResource();
        // mock rules
        final Rules mockRules = mockRules();

        final AuditRequest auditRequest = ReadRequestCompleteAuditRequest.create(mockOriginalRequestId)
                .withUser(mockUser)
                .withLeafResource(mockResource)
                .withContext(mockContext)
                .withRulesApplied(mockRules)
                .withNumberOfRecordsReturned(TEST_NUMBER_OF_RECORDS_RETURNED)
                .withNumberOfRecordsProcessed(TEST_NUMBER_OF_RECORDS_PROCESSED);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockUser, Mockito.atLeastOnce()).getUserId();
        verify(mockResource, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getType();
        verify(mockContext, Mockito.atLeastOnce()).getPurpose();
        Assert.assertTrue(log.contains(TEST_USER_ID));
        Assert.assertTrue(log.contains(TEST_PURPOSE));
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(TEST_DATA_TYPE));
        Assert.assertTrue(log.contains(TEST_RULES_APPLIED));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_COMPLETED_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_COMPLETED_DESCRIPTION));
    }

    @Test
    public void auditReadRequestTokenException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }

        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock resource
        final LeafResource mockResource = mockResource();
        // mock exception
        final Exception mockException = Mockito.mock(Exception.class);
        Mockito.doReturn(PalisadeService.TOKEN_NOT_FOUND_MESSAGE).when(mockException).getMessage();

        final AuditRequest auditRequest = ReadRequestExceptionAuditRequest.create(mockOriginalRequestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(mockResource)
                .withException(mockException);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getType();
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(TEST_DATA_TYPE));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_DESCRIPTION));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_EXCEPTION_TOKEN_OUTCOME_DESCRIPTION));
    }

    @Test
    public void auditReadRequestOtherException() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log = logger;
            log.addAppender(appender);
        } else {
            fail("Expected Logback logger");
        }

        // mock original request id
        final RequestId mockOriginalRequestId = mockOriginalRequestId();
        // mock resource
        final LeafResource mockResource = mockResource();
        // mock exception
        final Exception mockException = mockException();

        final AuditRequest auditRequest = ReadRequestExceptionAuditRequest.create(mockOriginalRequestId)
                .withToken(TEST_TOKEN)
                .withLeafResource(mockResource)
                .withException(mockException);

        // When
        STROOM_AUDIT_SERVICE.audit(auditRequest);

        //Then
        verify(appender, atLeastOnce()).doAppend(logCaptor.capture());
        final String log = logCaptor.getValue().getFormattedMessage();
        verify(mockOriginalRequestId, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getId();
        verify(mockResource, Mockito.atLeastOnce()).getType();
        verify(mockException, Mockito.atLeastOnce()).getMessage();
        Assert.assertTrue(log.contains(TEST_ORIGINAL_REQUEST_ID));
        Assert.assertTrue(log.contains(TEST_RESOURCE_ID));
        Assert.assertTrue(log.contains(TEST_DATA_TYPE));
        Assert.assertTrue(log.contains(TEST_EXCEPTION_MESSAGE));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_EXCEPTION_OTHER_TYPE_ID));
        Assert.assertTrue(log.contains(StroomAuditService.READ_REQUEST_EXCEPTION_OTHER_DESCRIPTION));
    }
}
