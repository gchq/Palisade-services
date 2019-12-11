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

package uk.gov.gchq.palisade.service.resource.web;

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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class ResourceControllerTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private ResourceController controller;

    @Mock
    ResourceService resourceService;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ResourceController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller = new ResourceController(resourceService);
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
    public void infoOnGetByIdRequest() {
        // Given
        GetResourcesByIdRequest request = Mockito.mock(GetResourcesByIdRequest.class);
        Map<LeafResource, ConnectionDetail> response = Mockito.mock(Map.class);
        Mockito.when(resourceService.getResourcesById(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.getResourcesById(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

    @Test
    public void infoOnGetByResourceRequest() {
        // Given
        GetResourcesByResourceRequest request = Mockito.mock(GetResourcesByResourceRequest.class);
        Map<LeafResource, ConnectionDetail> response = Mockito.mock(Map.class);
        Mockito.when(resourceService.getResourcesByResource(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.getResourcesByResource(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

    @Test
    public void infoOnGetByTypeRequest() {
        // Given
        GetResourcesByTypeRequest request = Mockito.mock(GetResourcesByTypeRequest.class);
        Map<LeafResource, ConnectionDetail> response = Mockito.mock(Map.class);
        Mockito.when(resourceService.getResourcesByType(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.getResourcesByType(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }

    @Test
    public void infoOnGetBySerialisedFormatRequest() {
        // Given
        GetResourcesBySerialisedFormatRequest request = Mockito.mock(GetResourcesBySerialisedFormatRequest.class);
        Map<LeafResource, ConnectionDetail> response = Mockito.mock(Map.class);
        Mockito.when(resourceService.getResourcesBySerialisedFormat(request)).thenReturn(CompletableFuture.supplyAsync(() -> response));

        // When
        controller.getResourcesBySerialisedFormat(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));
    }
}
