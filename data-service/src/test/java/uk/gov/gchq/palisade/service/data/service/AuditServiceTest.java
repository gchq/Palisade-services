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

package uk.gov.gchq.palisade.service.data.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.data.request.AuditRequest;
import uk.gov.gchq.palisade.service.data.web.AuditClient;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class AuditServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private AuditService auditService;

    @Mock
    AuditClient auditClient;
    @Mock
    Executor executor;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AuditService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Supplier<URI> uriSupplier = () -> {
            try {
                return new URI("audit-service");
            } catch (Exception e) {
                return null;
            }
        };
        auditService = new AuditService(auditClient, uriSupplier, executor);
    }

    @After
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
    public void infoOnAuditRequest() {
        // Given
        AuditRequest request = Mockito.mock(AuditRequest.class);
        Boolean response = true;
        Mockito.when(auditClient.audit(Mockito.any(), Mockito.eq(request))).thenReturn(response);

        // When
        auditService.audit(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }
}
