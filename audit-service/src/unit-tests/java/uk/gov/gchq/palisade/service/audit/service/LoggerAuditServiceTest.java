/*
 * Copyright 2018-2021 Crown Copyright
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import uk.gov.gchq.palisade.service.audit.model.AuditMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_TOKEN;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditErrorMessage;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditSuccessMessage;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.DATA_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.FILTERED_RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.POLICY_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.USER_SERVICE;

@ExtendWith(MockitoExtension.class)
class LoggerAuditServiceTest {

    private static final String BAD_AUDIT_SUCCESS_MESSAGE = "An AuditSuccessMessage should only be sent by the 'Filtered Resource Service' or the 'Data Service'. Message received from {}";

    @Mock
    Logger logger;

    @Captor
    ArgumentCaptor<AuditMessage> infoCaptor;
    @Captor
    ArgumentCaptor<String> errorCaptor;

    private LoggerAuditService auditService;

    @BeforeEach
    public void setUp() {
        this.auditService = new LoggerAuditService(logger);
    }

    @Test
    void testDataServiceAuditSuccessMessage() {
        // Given
        doNothing().when(logger).info(anyString(), infoCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(DATA_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that an AuditSuccessMessage from the 'Data Service' is logged")
                .containsOnly(auditSuccessMessage(DATA_SERVICE));
    }

    @Test
    void testFilteredResourceServiceAuditSuccessMessage() {
        // Given
        doNothing().when(logger).info(anyString(), infoCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(FILTERED_RESOURCE_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that an AuditSuccessMessage from the 'Filtered Resource Service' is logged")
                .containsOnly(auditSuccessMessage(FILTERED_RESOURCE_SERVICE));
    }

    @Test
    void testOtherServiceAuditSuccessMessage() {
        // Given
        doNothing().when(logger).warn(errorCaptor.capture(), errorCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(USER_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that no AuditSuccessMessage object has been logged")
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .as("Check that a warning message has been logged when an AuditSuccessMessage has been sent by an incorrect service")
                .containsOnly(BAD_AUDIT_SUCCESS_MESSAGE, USER_SERVICE.value);
    }

    @Test
    void testUserServiceAuditErrorMessage() {

        // Given
        doNothing().when(logger).error(anyString(), errorCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditErrorMessage(USER_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that no info messages were produced")
                .isEmpty();

        assertThat(errorCaptor.getAllValues())
                .as("Check the logged AuditErrorMessage is from the 'User Service'")
                .hasSize(1)
                .first()
                .isEqualTo(auditErrorMessage(USER_SERVICE));
    }

    @Test
    void testResourceServiceAuditErrorMessage() {

        // Given
        doNothing().when(logger).error(anyString(), errorCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditErrorMessage(RESOURCE_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that no info messages were produced")
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .as("Check the logged AuditErrorMessage is from the 'Resource Service'")
                .hasSize(1)
                .first()
                .isEqualTo(auditErrorMessage(RESOURCE_SERVICE));
    }

    @Test
    void testOtherServiceAuditErrorMessage() {
        // Given
        doNothing().when(logger).error(anyString(), errorCaptor.capture());

        // When
        auditService.audit(TEST_TOKEN, auditErrorMessage(POLICY_SERVICE));

        // Then
        assertThat(infoCaptor.getAllValues())
                .as("Check that no info messages were produced")
                .isEmpty();
        assertThat(errorCaptor.getAllValues())
                .as("Check the logged AuditErrorMessage is from the 'Policy Service'")
                .hasSize(1)
                .first()
                .isEqualTo(auditErrorMessage(POLICY_SERVICE));
    }
}
