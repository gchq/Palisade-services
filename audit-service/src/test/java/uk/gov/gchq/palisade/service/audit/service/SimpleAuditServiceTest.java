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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class SimpleAuditServiceTest extends AuditServiceTestCommon {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    private static AuditService auditService;

    private static UserId userId = mockUserID();
    private static User user = mockUser();
    private static Context context = mockContext();
    private static RequestId requestId = mockOriginalRequestId();
    private static LeafResource resource = mockResource();
    private static Exception exception = mockException();
    private static Rules rules = mockRules();

    @Before
    public void setUp() {
        auditService = new SimpleAuditService();

        logger = (Logger) LoggerFactory.getLogger(SimpleAuditService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @DataPoints
    public static AuditRequest[] requests = new AuditRequest[] {
            RegisterRequestCompleteAuditRequest.create(requestId)
                    .withUser(user)
                    .withLeafResources(Sets.newSet(resource))
                    .withContext(context),
            RegisterRequestExceptionAuditRequest.create(requestId)
                    .withUserId(userId)
                    .withResourceId(resource.getId())
                    .withContext(context)
                    .withException(exception)
                    .withServiceClass(Service.class),
            ReadRequestCompleteAuditRequest.create(requestId)
                    .withUser(user)
                    .withLeafResource(resource)
                    .withContext(context)
                    .withRulesApplied(rules)
                    .withNumberOfRecordsReturned(TEST_NUMBER_OF_RECORDS_RETURNED)
                    .withNumberOfRecordsProcessed(TEST_NUMBER_OF_RECORDS_PROCESSED),
            ReadRequestExceptionAuditRequest.create(requestId)
                    .withToken(TEST_TOKEN)
                    .withLeafResource(resource)
                    .withException(exception)
    };

    private List<String> getMessages(Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Theory
    public void auditOnAuditRequest(AuditRequest request) {
        // When
        auditService.audit(request);

        // Then
        List<String> logMessages = getMessages(event -> true);

        MatcherAssert.assertThat(logMessages, Matchers.hasItems(
                Matchers.containsString(request.toString())
        ));
    }

}
