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

import event.logging.Event;
import event.logging.impl.DefaultEventLoggingService;
import event.logging.impl.DefaultEventSerializer;
import event.logging.impl.EventSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.gchq.palisade.service.audit.ApplicationTestData;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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
        // Given

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.DATA_SERVICE.value));

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());

        assertAll(
                () -> assertThat(logCaptor.getAllValues()).hasSize(1),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getHostName()).isEqualTo(ApplicationTestData.TEST_SERVER_NAME),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getIPAddress()).isEqualTo(ApplicationTestData.TEST_SERVER_IP),
                () -> assertThat(logCaptor.getValue().getEventSource().getUser().getId()).isEqualTo(ApplicationTestData.TEST_USER_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getTypeId()).isEqualTo(ServiceName.DATA_SERVICE.value),
                () -> assertThat(logCaptor.getValue().getEventDetail().getDescription()).isEqualTo(StroomAuditService.READ_SUCCESS),
                () -> assertThat(logCaptor.getValue().getEventDetail().getPurpose().getJustification()).isEqualTo(ApplicationTestData.TEST_PURPOSE),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getObjects().get(0).getId()).isEqualTo(ApplicationTestData.TEST_LEAF_RESOURCE_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().isSuccess()).isTrue(),
                () -> assertThat(logCaptor.getValue().getEventChain().getActivity().getId()).isEqualTo(ApplicationTestData.TEST_TOKEN)
        );
    }

    @Test
    void testFilteredResourceServiceAuditSuccessMessage() {
        // Given

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.FILTERED_RESOURCE_SERVICE.value));

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());

        assertAll(
                () -> assertThat(logCaptor.getAllValues()).hasSize(1),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getHostName()).isEqualTo(ApplicationTestData.TEST_SERVER_NAME),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getIPAddress()).isEqualTo(ApplicationTestData.TEST_SERVER_IP),
                () -> assertThat(logCaptor.getValue().getEventSource().getUser().getId()).isEqualTo(ApplicationTestData.TEST_USER_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getTypeId()).isEqualTo(ServiceName.FILTERED_RESOURCE_SERVICE.value),
                () -> assertThat(logCaptor.getValue().getEventDetail().getDescription()).isEqualTo(StroomAuditService.REQUEST_SUCCESS),
                () -> assertThat(logCaptor.getValue().getEventDetail().getPurpose().getJustification()).isEqualTo(ApplicationTestData.TEST_PURPOSE),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getObjects().get(0).getId()).isEqualTo(ApplicationTestData.TEST_LEAF_RESOURCE_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().isSuccess()).isTrue(),
                () -> assertThat(logCaptor.getValue().getEventChain().getActivity().getId()).isEqualTo(ApplicationTestData.TEST_TOKEN)
        );
    }

    @Test
    void testOtherServiceAuditSuccessMessage() {
        // Given

        // When
        Boolean result = auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage(ServiceName.USER_SERVICE.value));

        //Then
        assertThat(result).isFalse();
    }

    @Test
    void testUserServiceAuditErrorMessage() {
        // Given
        AuditErrorMessage message = ApplicationTestData.auditErrorMessage(ServiceName.USER_SERVICE.value);

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());

        assertAll(
                () -> assertThat(logCaptor.getAllValues()).hasSize(1),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getHostName()).isEqualTo(ApplicationTestData.TEST_SERVER_NAME),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getIPAddress()).isEqualTo(ApplicationTestData.TEST_SERVER_IP),
                () -> assertThat(logCaptor.getValue().getEventSource().getUser().getId()).isEqualTo(ApplicationTestData.TEST_USER_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getTypeId()).isEqualTo(ServiceName.USER_SERVICE.value),
                () -> assertThat(logCaptor.getValue().getEventDetail().getDescription()).isEqualTo(message.getErrorNode().get("stackTrace").get(0).get("className").textValue()),
                () -> assertThat(logCaptor.getValue().getEventDetail().getPurpose().getJustification()).isEqualTo(ApplicationTestData.TEST_PURPOSE),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getObjects().get(0).getId()).isEqualTo(ApplicationTestData.TEST_RESOURCE_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().isSuccess()).isFalse(),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().getDescription()).isEqualTo(message.getErrorNode().get("message").textValue()),
                () -> assertThat(logCaptor.getValue().getEventChain().getActivity().getId()).isEqualTo(ApplicationTestData.TEST_TOKEN)
        );
    }

    @Test
    void testResourceServiceAuditErrorMessage() {
        // Given
        AuditErrorMessage message = ApplicationTestData.auditErrorMessage(ServiceName.RESOURCE_SERVICE.value);

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());

        assertAll(
                () -> assertThat(logCaptor.getAllValues()).hasSize(1),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getHostName()).isEqualTo(ApplicationTestData.TEST_SERVER_NAME),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getIPAddress()).isEqualTo(ApplicationTestData.TEST_SERVER_IP),
                () -> assertThat(logCaptor.getValue().getEventSource().getUser().getId()).isEqualTo(ApplicationTestData.TEST_USER_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getTypeId()).isEqualTo(ServiceName.RESOURCE_SERVICE.value),
                () -> assertThat(logCaptor.getValue().getEventDetail().getDescription()).isEqualTo(message.getErrorNode().get("stackTrace").get(0).get("className").textValue()),
                () -> assertThat(logCaptor.getValue().getEventDetail().getPurpose().getJustification()).isEqualTo(ApplicationTestData.TEST_PURPOSE),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getObjects().get(0).getId()).isEqualTo(ApplicationTestData.TEST_RESOURCE_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().isSuccess()).isFalse(),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().getDescription()).isEqualTo(message.getErrorNode().get("message").textValue()),
                () -> assertThat(logCaptor.getValue().getEventChain().getActivity().getId()).isEqualTo(ApplicationTestData.TEST_TOKEN)
        );
    }

    @Test
    void testOtherServiceAuditErrorMessage() {
        // Given
        AuditErrorMessage message = ApplicationTestData.auditErrorMessage(ServiceName.POLICY_SERVICE.value);

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, message);

        //Then
        verify(eventLogger, atLeastOnce()).log(logCaptor.capture());

        assertAll(
                () -> assertThat(logCaptor.getAllValues()).hasSize(1),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getHostName()).isEqualTo(ApplicationTestData.TEST_SERVER_NAME),
                () -> assertThat(logCaptor.getValue().getEventSource().getDevice().getIPAddress()).isEqualTo(ApplicationTestData.TEST_SERVER_IP),
                () -> assertThat(logCaptor.getValue().getEventSource().getUser().getId()).isEqualTo(ApplicationTestData.TEST_USER_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getTypeId()).isEqualTo(ServiceName.POLICY_SERVICE.value),
                () -> assertThat(logCaptor.getValue().getEventDetail().getDescription()).isEqualTo(message.getErrorNode().get("stackTrace").get(0).get("className").textValue()),
                () -> assertThat(logCaptor.getValue().getEventDetail().getPurpose().getJustification()).isEqualTo(ApplicationTestData.TEST_PURPOSE),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getObjects().get(0).getId()).isEqualTo(ApplicationTestData.TEST_RESOURCE_ID),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().isSuccess()).isFalse(),
                () -> assertThat(logCaptor.getValue().getEventDetail().getAuthorise().getOutcome().getDescription()).isEqualTo(message.getErrorNode().get("message").textValue()),
                () -> assertThat(logCaptor.getValue().getEventChain().getActivity().getId()).isEqualTo(ApplicationTestData.TEST_TOKEN)
        );
    }
}
