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

import event.logging.BaseOutcome;
import event.logging.Event;
import event.logging.impl.DefaultEventLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_EXCEPTION;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_LEAF_RESOURCE_ID;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_PURPOSE;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_RESOURCE_ID;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_SERVER_IP;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_SERVER_NAME;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_TOKEN;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_USER_ID;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditErrorMessage;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditSuccessMessage;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.DATA_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.FILTERED_RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.POLICY_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.RESOURCE_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.USER_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.StroomAuditService.READ_SUCCESS;
import static uk.gov.gchq.palisade.service.audit.service.StroomAuditService.REQUEST_SUCCESS;

@ExtendWith(MockitoExtension.class)
class StroomAuditServiceTest {

    @Spy
    DefaultEventLoggingService eventLogger = new DefaultEventLoggingService();
    @Captor
    ArgumentCaptor<Event> logCaptor;

    private static StroomAuditService auditService;

    @BeforeEach
    public void setUp() {
        auditService = new StroomAuditService(eventLogger)
                .organisation("Test Org")
                .systemClassification("Some system classification")
                .systemDescription("some system description")
                .systemEnv("some system env")
                .systemName("some system name")
                .systemVersion("some system version");
    }

    @Test
    void testDataServiceAuditSuccessMessage() {

        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(DATA_SERVICE.value));

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .as("Check that one log message is produced")
                .hasSize(1);

        var event = logCaptor.getValue();
        var eventDetail = event.getEventDetail();

        assertThat(event.getEventSource())
                .as("Check that the eventSource has been created successfully")
                .extracting("device.hostName", "device.ipAddress", "user.id")
                .contains(TEST_SERVER_NAME, TEST_SERVER_IP, TEST_USER_ID);

        assertThat(event.getEventChain())
                .as("Check that the activity id is the test token")
                .extracting("activity.id")
                .isEqualTo(TEST_TOKEN);

        assertThat(eventDetail)
                .as("Check that the event detail has been populated correctly")
                .extracting("typeId", "description", "purpose.justification")
                .contains(DATA_SERVICE.value, READ_SUCCESS, TEST_PURPOSE);

        assertThat(eventDetail.getAuthorise().getObjects().get(0).getId())
                .as("Check that the authorise contains the leaf resource Id")
                .isEqualTo(TEST_LEAF_RESOURCE_ID);

        assertThat(eventDetail.getAuthorise().getOutcome().isSuccess())
                .as("Check that the outcome is a success")
                .isTrue();
    }

    @Test
    void testFilteredResourceServiceAuditSuccessMessage() {
        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(FILTERED_RESOURCE_SERVICE));

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .as("Check that one log message is produced")
                .hasSize(1);

        var event = logCaptor.getValue();
        var eventDetail = event.getEventDetail();

        assertThat(event.getEventSource())
                .as("Check that the eventSource has been created successfully")
                .extracting("device.hostName", "device.ipAddress", "user.id")
                .contains(TEST_SERVER_NAME, TEST_SERVER_IP, TEST_USER_ID);

        assertThat(event.getEventChain())
                .as("Check that the activity id is the test token")
                .extracting("activity.id")
                .isEqualTo(TEST_TOKEN);

        assertThat(eventDetail)
                .as("Check that the event detail has been populated correctly")
                .extracting("typeId", "description", "purpose.justification")
                .contains(FILTERED_RESOURCE_SERVICE.value, REQUEST_SUCCESS, TEST_PURPOSE);

        assertThat(eventDetail.getAuthorise().getObjects().get(0).getId())
                .as("Check that the authorise contains the leaf resource Id")
                .isEqualTo(TEST_LEAF_RESOURCE_ID);

        assertThat(eventDetail.getAuthorise().getOutcome().isSuccess())
                .as("Check that the outcome is a success")
                .isTrue();
    }

    @Test
    void testOtherServiceAuditSuccessMessage() {
        // Given

        // When
        Boolean result = auditService.audit(TEST_TOKEN, auditSuccessMessage(USER_SERVICE.value));

        //Then
        assertThat(result)
                .as("Check that an error message from a non accepted service is not logged")
                .isFalse();
    }

    @Test
    void testUserServiceAuditErrorMessage() {
        // Given
        var message = auditErrorMessage(USER_SERVICE.value);

        // When
        auditService.audit(TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .as("Check that one log message is produced")
                .hasSize(1);

        var event = logCaptor.getValue();
        var eventDetail = event.getEventDetail();

        assertThat(event.getEventSource())
                .as("Check that the eventSource has been created successfully")
                .extracting("device.hostName", "device.ipAddress", "user.id")
                .contains(TEST_SERVER_NAME, TEST_SERVER_IP, TEST_USER_ID);

        assertThat(event.getEventChain())
                .as("Check that the activity id is the test token")
                .extracting("activity.id")
                .isEqualTo(TEST_TOKEN);

        assertThat(eventDetail)
                .as("Check that the event detail has been populated correctly")
                .extracting("typeId", "description", "purpose.justification")
                .contains(USER_SERVICE.value, message.getErrorNode().get("stackTrace").get(0).get("className").textValue(), TEST_PURPOSE);

        assertThat(eventDetail.getAuthorise().getObjects().get(0).getId())
                .as("Check that the authorise contains the leaf resource Id")
                .isEqualTo(TEST_RESOURCE_ID);

        assertThat(eventDetail.getAuthorise().getOutcome().isSuccess())
                .as("Check that the outcome is a failure")
                .isFalse();

        assertThat(eventDetail.getAuthorise().getOutcome())
                .as("Check that the outcome is a failure")
                .extracting(BaseOutcome::getDescription)
                .isEqualTo(TEST_EXCEPTION.getMessage());

    }

    @Test
    void testResourceServiceAuditErrorMessage() {
        // Given
        AuditErrorMessage message = auditErrorMessage(RESOURCE_SERVICE.value);

        // When
        auditService.audit(TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .as("Check that one log message is produced")
                .hasSize(1);

        var event = logCaptor.getValue();
        var eventDetail = event.getEventDetail();

        assertThat(event.getEventSource())
                .as("Check that the eventSource has been created successfully")
                .extracting("device.hostName", "device.ipAddress", "user.id")
                .contains(TEST_SERVER_NAME, TEST_SERVER_IP, TEST_USER_ID);

        assertThat(event.getEventChain())
                .as("Check that the activity id is the test token")
                .extracting("activity.id")
                .isEqualTo(TEST_TOKEN);

        assertThat(eventDetail)
                .as("Check that the event detail has been populated correctly")
                .extracting("typeId", "description", "purpose.justification")
                .contains(RESOURCE_SERVICE.value, message.getErrorNode().get("stackTrace").get(0).get("className").textValue(), TEST_PURPOSE);

        assertThat(eventDetail.getAuthorise().getObjects().get(0).getId())
                .as("Check that the authorise contains the leaf resource Id")
                .isEqualTo(TEST_RESOURCE_ID);

        assertThat(eventDetail.getAuthorise().getOutcome().isSuccess())
                .as("Check that the outcome is a failure")
                .isFalse();

        assertThat(eventDetail.getAuthorise().getOutcome())
                .as("Check that the outcome is a failure")
                .extracting(BaseOutcome::getDescription)
                .isEqualTo(TEST_EXCEPTION.getMessage());
    }

    @Test
    void testOtherServiceAuditErrorMessage() {
        // Given
        AuditErrorMessage message = auditErrorMessage(POLICY_SERVICE.value);

        // When
        auditService.audit(TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .as("Check that one log message is produced")
                .hasSize(1);

        var event = logCaptor.getValue();
        var eventDetail = event.getEventDetail();

        assertThat(event.getEventSource())
                .as("Check that the eventSource has been created successfully")
                .extracting("device.hostName", "device.ipAddress", "user.id")
                .contains(TEST_SERVER_NAME, TEST_SERVER_IP, TEST_USER_ID);

        assertThat(event.getEventChain())
                .as("Check that the activity id is the test token")
                .extracting("activity.id")
                .isEqualTo(TEST_TOKEN);

        assertThat(eventDetail)
                .as("Check that the event detail has been populated correctly")
                .extracting("typeId", "description", "purpose.justification")
                .contains(POLICY_SERVICE.value, message.getErrorNode().get("stackTrace").get(0).get("className").textValue(), TEST_PURPOSE);

        assertThat(eventDetail.getAuthorise().getObjects().get(0).getId())
                .as("Check that the authorise contains the leaf resource Id")
                .isEqualTo(TEST_RESOURCE_ID);

        assertThat(eventDetail.getAuthorise().getOutcome().isSuccess())
                .as("Check that the outcome is a failure")
                .isFalse();

        assertThat(eventDetail.getAuthorise().getOutcome())
                .as("Check that the outcome is a failure")
                .extracting(BaseOutcome::getDescription)
                .isEqualTo(TEST_EXCEPTION.getMessage());
    }
}
