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
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.request.AuditRequest;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class SimpleAuditServiceTest extends AuditServiceTestCommon {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    private static AuditService auditService;

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

    //@DataPoints requires the property to be public
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @DataPoints()
    public static AuditRequest[] requests = new AuditRequest[] {
            registerRequestCompleteAuditRequest(),
            registerRequestExceptionAuditRequest(),
            readRequestCompleteAuditRequest(),
            readRequestExceptionAuditRequest()

    };

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Theory
    public void auditOnAuditRequest(final AuditRequest request) {
        // When
        auditService.audit(request);

        // Then
        List<String> logMessages = getMessages(event -> true);

        MatcherAssert.assertThat(logMessages, Matchers.hasItems(
                Matchers.containsString(request.toString())
        ));
    }

}
