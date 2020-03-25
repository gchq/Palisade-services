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

package uk.gov.gchq.palisade.service.palisade.web;

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

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class PalisadeControllerTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private PalisadeController controller;

    @Mock
    PalisadeService palisadeService;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PalisadeController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller = new PalisadeController(palisadeService, JSONSerialiser.createDefaultMapper());
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
    public void infoOnRegisterDataRequest() {
        // Given
        RegisterDataRequest request = Mockito.mock(RegisterDataRequest.class);
        DataRequestResponse response = Mockito.mock(DataRequestResponse.class);
        Mockito.when(palisadeService.registerDataRequest(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.registerDataRequestSync(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

    @Test
    public void infoOnGetDataRequestConfig() throws ExecutionException, InterruptedException {
        // Given
        GetDataRequestConfig request = Mockito.mock(GetDataRequestConfig.class);
        DataRequestConfig response = Mockito.mock(DataRequestConfig.class);
        Mockito.when(palisadeService.getDataRequestConfig(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.getDataRequestConfigSync(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }
}
