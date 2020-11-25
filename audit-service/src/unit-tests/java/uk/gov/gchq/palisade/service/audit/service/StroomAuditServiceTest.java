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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class StroomAuditServiceTest {

    private static final String TOKEN_NOT_FOUND_MESSAGE = "User's request was not in the cache: ";

    @Spy
    DefaultEventLoggingService eventLogger = new DefaultEventLoggingService();
    @Captor
    ArgumentCaptor<Event> logCaptor;

    private static final EventSerializer EVENT_SERIALIZER = new DefaultEventSerializer();
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
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditSuccessMessage("DATA_SERVICE"));

        //Then
        final String log = EVENT_SERIALIZER.serialize(logCaptor.getValue());

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(ApplicationTestData.TEST_USER_ID)),
                () -> assertThat(logCaptor.getAllValues().contains(ApplicationTestData.TEST_PURPOSE)),
                () -> assertThat(logCaptor.getAllValues().contains(ApplicationTestData.TEST_RESOURCE_ID)),
                () -> assertThat(logCaptor.getAllValues().contains(StroomAuditService.AUDIT_SUCCESS_REQUEST_NO_RESOURCES_TYPE_ID)),
                () -> assertThat(logCaptor.getAllValues().contains(StroomAuditService.AUDIT_SUCCESS_REQUEST_NO_RESOURCES_DESCRIPTION)),
                () -> assertThat(logCaptor.getAllValues().contains(StroomAuditService.AUDIT_SUCCESS_REQUEST_NO_RESOURCES_OUTCOME_DESCRIPTION))
        );
    }

    /*@Test
    void testAuditErrorMessage() {
        // Given

        // When
        auditService.audit(ApplicationTestData.TEST_TOKEN, ApplicationTestData.auditErrorMessage());

        //Then
        final String log = eventSerializer.serialize(logCaptor.getValue());

        assertAll(
                () -> assertThat(logCaptor.getAllValues().contains(userId.getId())),
                () -> assertThat(logCaptor.getAllValues().contains(context.getPurpose())),
                () -> assertThat(logCaptor.getAllValues().contains(requestId.getId())),
                () -> assertThat(logCaptor.getAllValues().contains(resource.getId())),
                () -> assertThat(logCaptor.getAllValues().contains(resource.getType())),
                () -> assertThat(logCaptor.getAllValues().contains(StroomAuditService.AUDIT_SUCCESS_REQUEST_ID)),
                () -> assertThat(logCaptor.getAllValues().contains(StroomAuditService.AUDIT_SUCCESS_REQUEST_DESCRIPTION))
        );
    }*/
}
