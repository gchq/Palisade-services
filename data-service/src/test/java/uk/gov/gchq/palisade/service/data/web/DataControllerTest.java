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

package uk.gov.gchq.palisade.service.data.web;

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

import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.request.ReadResponse;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class DataControllerTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private DataController controller;

    @Mock
    DataService dataService;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(DataController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller = new DataController(dataService);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    public void infoOnReadRequest() {
        // Given
        ReadRequest request = Mockito.mock(ReadRequest.class);
        ReadResponse response = Mockito.mock(ReadResponse.class);
        Mockito.when(dataService.read(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.readSync(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

    @Test
    public void infoOnReadChunkedRequest() throws IOException {
        // Given
        ReadRequest request = Mockito.mock(ReadRequest.class);
        ReadResponse response = Mockito.mock(ReadResponse.class);
        InputStream input = Mockito.mock(InputStream.class);
        OutputStream output = Mockito.mock(OutputStream.class);
        Mockito.when(dataService.read(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));
        Mockito.when(response.asInputStream()).thenReturn(input);

        // When
        controller.readChunked(request).getBody().writeTo(output);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

}
