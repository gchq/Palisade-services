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

package uk.gov.gchq.palisade.service.data.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.config.StdSerialiserConfiguration;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class DataControllerTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private DataController controller;

    DataService dataService = Mockito.mock(DataService.class);
    AuditService auditService = Mockito.mock(AuditService.class);
    StdSerialiserConfiguration config = Mockito.mock(StdSerialiserConfiguration.class);

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(DataController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller = new DataController(dataService, auditService, config);
    }

    @AfterEach
    void tearDown() {
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
    void infoOnReadChunkedRequest() throws IOException {
        // Given
        DataRequest request = DataRequest.Builder.create()
                .withToken("test-request-token")
                .withLeafResourceId("/resource/id");
        DataReaderRequest readerRequest = Mockito.mock(DataReaderRequest.class);
        OutputStream output = Mockito.mock(OutputStream.class);

        // Given the service is mocked
        Mockito.when(dataService.authoriseRequest(Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(readerRequest)));
        Mockito.when(dataService.read(Mockito.any(), Mockito.any()))
                .thenReturn(Pair.of(new AtomicLong(1), new AtomicLong(1)));

        // When
        controller.readChunked(request).getBody().writeTo(output);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString())
        ));

        List<String> errorMessages = getMessages(event -> event.getLevel() == Level.WARN || event.getLevel() == Level.ERROR);
        MatcherAssert.assertThat(errorMessages, Matchers.empty());
    }

}
