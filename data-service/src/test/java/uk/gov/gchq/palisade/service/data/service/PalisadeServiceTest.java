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

import uk.gov.gchq.palisade.service.data.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.data.web.PalisadeClient;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class PalisadeServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private PalisadeService palisadeService;

    @Mock
    PalisadeClient palisadeClient;
    Executor executor = Executors.newSingleThreadExecutor();

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PalisadeService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        palisadeService = new PalisadeService(palisadeClient, executor);
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
    public void infoOnGetDataRequestConfig() throws ExecutionException, InterruptedException {
        // Given
        GetDataRequestConfig request = Mockito.mock(GetDataRequestConfig.class);
        DataRequestConfig response = Mockito.mock(DataRequestConfig.class);
        Mockito.when(palisadeClient.getDataRequestConfig(request)).thenReturn(response);

        // When
        palisadeService.getDataRequestConfig(request).join();

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));

        List<String> errorMessages = getMessages(event -> event.getLevel() == Level.WARN || event.getLevel() == Level.ERROR);
        MatcherAssert.assertThat(errorMessages, Matchers.empty());
    }
}
