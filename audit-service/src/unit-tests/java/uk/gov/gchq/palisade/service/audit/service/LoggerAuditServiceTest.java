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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import uk.gov.gchq.palisade.service.audit.ApplicationTestData;
import uk.gov.gchq.palisade.service.audit.model.AuditMessage;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LoggerAuditServiceTest {

    private static final String AUDIT_ERROR_MESSAGE = "AuditMessage : AuditErrorMessage";
    private static final String AUDIT_ERROR_FROM = "auditErrorMessage from %s";
    private static final String AUDIT_SUCCESS_MESSAGE = "AuditMessage : AuditSuccessMessage";
    private static final String AUDIT_SUCCESS_FROM = "auditSuccessMessage from %s";
    private static final String BAD_AUDIT_SUCCESS_MESSAGE = "An AuditSuccessMessage should only be sent by the FILTERED_RESOURCE_SERVICE or the DATA_SERVICE. Message received from USER_SERVICE";
    private static final String REQUEST_FOR_TOKEN = "LoggerAuditService received an audit request for token 'token in the form of a UUID'";

    @Mock
    Logger logger;

    @Captor
    ArgumentCaptor<AuditMessage> infoCaptor;
    @Captor
    ArgumentCaptor<String> errorCaptor;

    private static LoggerAuditService auditService;

    @BeforeEach
    public void setUp() {
        auditService = new LoggerAuditService(logger);
    }

    @Test
    void testDataServiceAuditSuccessMessage() {
        // Given
        Mockito.doNothing().when(logger).info(Mockito.anyString(), infoCaptor.capture());
        // Given
        Mockito.doNothing().when(logger).warn(errorCaptor.capture());
        Mockito.doNothing().when(logger).error(errorCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage("DATA_SERVICE"));

        // Then
        System.out.println("Log Captor: " + infoCaptor.getAllValues());
        assertThat(infoCaptor.getAllValues())
                .hasSize(1)
                .first().isEqualTo(ApplicationTestData.auditSuccessMessage("DATA_SERVICE"));
        assertThat(errorCaptor.getAllValues())
                .isEmpty();
        /*assertAll(
                () -> assertThat(logCaptor.getValue()).isEqualTo(REQUEST_FOR_TOKEN)
                () -> assertThat(logMessages.get(1)).contains(String.format(AUDIT_SUCCESS_FROM, "DATA_SERVICE")),
                () -> assertThat(logMessages.get(2)).contains(
                        AUDIT_SUCCESS_MESSAGE, ApplicationTestData.TEST_USER_ID,
                        ApplicationTestData.TEST_RESOURCE_ID, ApplicationTestData.TEST_PURPOSE,
                        ApplicationTestData.TEST_TIMESTAMP, ApplicationTestData.TEST_SERVER_IP,
                        ApplicationTestData.TEST_SERVER_NAME, ApplicationTestData.TEST_ATTRIBUTES.get("test attribute key").toString(),
                        ApplicationTestData.TEST_LEAF_RESOURCE_ID
                ),
                () -> assertThat(logMessages.get(3)).isEqualTo(logMessages.get(2))
        );*/
    }

    /*@Test
    void testFilteredResourceServiceAuditSuccessMessage() {

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.filteredServiceAuditSuccessMessage());

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
    }*/
}
