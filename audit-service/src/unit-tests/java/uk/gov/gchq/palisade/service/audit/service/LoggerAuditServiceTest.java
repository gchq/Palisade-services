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

    private static final String BAD_AUDIT_SUCCESS_MESSAGE = "An AuditSuccessMessage should only be sent by the FILTERED_RESOURCE_SERVICE or the DATA_SERVICE. Message received from {}";

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

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.DATA_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .hasSize(1)
                .first().isEqualTo(ApplicationTestData.auditSuccessMessage("DATA_SERVICE"));
    }

    @Test
    void testFilteredResourceServiceAuditSuccessMessage() {
        // Given
        Mockito.doNothing().when(logger).info(Mockito.anyString(), infoCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.FILTERED_RESOURCE_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .hasSize(1).first()
                .isEqualTo(ApplicationTestData.auditSuccessMessage("FILTERED_RESOURCE_SERVICE"));
    }

    @Test
    void testOtherServiceAuditSuccessMessage() {
        // Given
        Mockito.doNothing().when(logger).warn(errorCaptor.capture(), errorCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.USER_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .hasSize(2).contains(BAD_AUDIT_SUCCESS_MESSAGE, "USER_SERVICE");
    }

    @Test
    void testUserServiceAuditErrorMessage() {
        // Given
        Mockito.doNothing().when(logger).error(Mockito.anyString(), errorCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditErrorMessage(ServiceName.USER_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .hasSize(1).first().isEqualTo(ApplicationTestData.auditErrorMessage("USER_SERVICE"));
    }

    @Test
    void testResourceServiceAuditErrorMessage() {
        // Given
        Mockito.doNothing().when(logger).error(Mockito.anyString(), errorCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditErrorMessage(ServiceName.RESOURCE_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .hasSize(1).first().isEqualTo(ApplicationTestData.auditErrorMessage("RESOURCE_SERVICE"));
    }

    @Test
    void testOtherServiceAuditErrorMessage() {
        // Given
        Mockito.doNothing().when(logger).error(Mockito.anyString(), errorCaptor.capture());

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditErrorMessage(ServiceName.POLICY_SERVICE.name()));

        // Then
        assertThat(infoCaptor.getAllValues())
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .hasSize(1).first().isEqualTo(ApplicationTestData.auditErrorMessage("POLICY_SERVICE"));
    }
}
