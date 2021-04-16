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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.audit.common.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.audit.model.AuditSuccessMessage;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.TEST_TOKEN;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditErrorMessage;
import static uk.gov.gchq.palisade.service.audit.ApplicationTestData.auditSuccessMessage;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.DATA_SERVICE;
import static uk.gov.gchq.palisade.service.audit.service.ServiceName.USER_SERVICE;

class SimpleAuditServiceTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    private static AuditService auditService;

    @BeforeEach
    public void setUp() {
        auditService = new SimpleAuditService();
        logger = (Logger) LoggerFactory.getLogger(SimpleAuditService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void testAuditErrorMessage() {
        // When
        auditService.audit(TEST_TOKEN, auditErrorMessage(USER_SERVICE));
        // Then
        assertThat(getMessages(event -> true).get(0))
                .as("Check that the message logged is a %s", AuditErrorMessage.class.getSimpleName())
                .contains(AuditErrorMessage.class.getSimpleName());
    }

    @Test
    void testAuditSuccessMessage() {
        // When
        auditService.audit(TEST_TOKEN, auditSuccessMessage(DATA_SERVICE));
        // Then
        assertThat(getMessages(event -> true).get(0))
                .as("Check that the message logged is a %s", AuditSuccessMessage.class.getSimpleName())
                .contains(AuditSuccessMessage.class.getSimpleName());
    }
}
