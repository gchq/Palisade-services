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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.ApplicationTestData;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class LoggerAuditServiceTest {

    private static LoggerAuditService auditService;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setUp() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggerAuditService.class);
        auditService = new LoggerAuditService(logger);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void testAuditSuccessMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo("LoggerAuditService received an audit request for token 'token in the form of a UUID'"),
                () -> assertThat(logMessages.get(1)).contains(ApplicationTestData.TEST_SUCCESS_SERVICE_NAME),
                () -> assertThat(logMessages.get(2)).contains(
                        "AuditMessage : AuditSuccessMessage", ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_LEAF_RESOURCE_ID
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }

    @Test
    void testAuditErrorMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditErrorMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo("LoggerAuditService received an audit request for token 'token in the form of a UUID'"),
                () -> assertThat(logMessages.get(1)).contains(ApplicationTestData.TEST_ERROR_SERVICE_NAME),
                () -> assertThat(logMessages.get(2)).contains(
                        "AuditMessage : AuditErrorMessage", ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_EXCEPTION.getMessage()
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }
}
