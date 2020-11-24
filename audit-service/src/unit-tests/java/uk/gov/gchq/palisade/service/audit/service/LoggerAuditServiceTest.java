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

    private static final String AUDIT_ERROR_MESSAGE = "AuditMessage : AuditErrorMessage";
    private static final String AUDIT_ERROR_FROM = "auditErrorMessage from %s";
    private static final String AUDIT_SUCCESS_MESSAGE = "AuditMessage : AuditSuccessMessage";
    private static final String AUDIT_SUCCESS_FROM = "auditSuccessMessage from %s";
    private static final String BAD_AUDIT_SUCCESS_MESSAGE = "An AuditSuccessMessage should only be sent by the FILTERED_RESOURCE_SERVICE or the DATA_SERVICE. Message received from USER_SERVICE";
    private static final String REQUEST_FOR_TOKEN = "LoggerAuditService received an audit request for token 'token in the form of a UUID'";
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
    void testDataServiceAuditSuccessMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.dataServiceAuditSuccessMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_SUCCESS_FROM, "DATA_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_SUCCESS_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_LEAF_RESOURCE_ID
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }

    @Test
    void testFilteredResourceServiceAuditSuccessMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.filteredServiceAuditSuccessMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_SUCCESS_FROM, "FILTERED_RESOURCE_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_SUCCESS_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_LEAF_RESOURCE_ID
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }

    @Test
    void testOtherServiceAuditSuccessMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.badAuditSuccessMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(BAD_AUDIT_SUCCESS_MESSAGE)
        );
    }

    @Test
    void testUserServiceAuditErrorMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.userServiceAuditErrorMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_ERROR_FROM, "USER_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_ERROR_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_EXCEPTION.getMessage()
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }

    @Test
    void testResourceServiceAuditErrorMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.resourceServiceAuditErrorMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_ERROR_FROM, "RESOURCE_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_ERROR_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_EXCEPTION.getMessage()
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }

    @Test
    void testOtherServiceAuditErrorMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.otherServiceAuditErrorMessage());
        List<String> logMessages = getMessages(event -> true);

        // Then
        assertAll(
                () -> assertThat(logMessages.get(0)).isEqualTo(REQUEST_FOR_TOKEN),
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_ERROR_FROM, "POLICY_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_ERROR_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_EXCEPTION.getMessage()
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );
    }
}
