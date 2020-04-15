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
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.request.Request;
import uk.gov.gchq.palisade.service.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByTypeRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class ResourceControllerTest {

    private static ResourceService resourceService = Mockito.mock(ResourceService.class);
    private static ConnectionDetail detail = new SimpleConnectionDetail().uri("data-service");
    private static LeafResource resource = new FileResource()
            .id("/system/file")
            .type("type")
            .serialisedFormat("format")
            .connectionDetail(detail)
            .parent(new SystemResource().id("/system"));
    private static Set<LeafResource> resourceSet = Collections.singleton(resource);

    //@DataPoints requires the property to be public
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @DataPoints("GetRequests")
    public static List<Request> requests = Arrays.asList(
            new GetResourcesByIdRequest().resourceId(resource.getId()),
            new GetResourcesByResourceRequest().resource(resource),
            new GetResourcesBySerialisedFormatRequest().serialisedFormat(resource.getSerialisedFormat()),
            new GetResourcesByTypeRequest().type(resource.getType()));

    //@DataPoints requires the property to be public
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @DataPoint("AddRequests")
    public static AddResourceRequest addRequest = new AddResourceRequest().resource(resource);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private ResourceController controller;
    private Map<Class<? extends Request>, Function<Request, Set<LeafResource>>> requestMethods = new HashMap<>();

    {
        requestMethods.put(GetResourcesByIdRequest.class, request -> {
            request.originalRequestId(new RequestId().id("originalId"));
            return controller.getResourcesById((GetResourcesByIdRequest) request);
        });
        requestMethods.put(GetResourcesByResourceRequest.class, request -> controller.getResourcesByResource((GetResourcesByResourceRequest) request));
        requestMethods.put(GetResourcesBySerialisedFormatRequest.class, request -> controller.getResourcesBySerialisedFormat((GetResourcesBySerialisedFormatRequest) request));
        requestMethods.put(GetResourcesByTypeRequest.class, request -> controller.getResourcesByType((GetResourcesByTypeRequest) request));
    }

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ResourceController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller = new ResourceController(resourceService);

        Mockito.when(resourceService.getResourcesByResource(resource)).thenReturn(resourceSet.stream());
        Mockito.when(resourceService.getResourcesById(resource.getId())).thenReturn(resourceSet.stream());
        Mockito.when(resourceService.getResourcesByType(resource.getType())).thenReturn(resourceSet.stream());
        Mockito.when(resourceService.getResourcesBySerialisedFormat(resource.getSerialisedFormat())).thenReturn(resourceSet.stream());
        Mockito.when(resourceService.addResource(resource)).thenReturn(true);
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

    @Theory
    public void infoOnGetResourceRequest(@FromDataPoints("GetRequests") final Request request) {
        // Given
        Function<Request, Set<LeafResource>> method = requestMethods.get(request.getClass());
        Set<LeafResource> expectedResponse = resourceSet;

        // When
        Set<LeafResource> response = method.apply(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));

        MatcherAssert.assertThat(response, Matchers.equalTo(expectedResponse));
    }

    @Theory
    public void infoOnAddResourceRequest(@FromDataPoints("AddRequests") final AddResourceRequest request) {
        // Given
        Boolean expectedResponse = true;

        // When
        Boolean response = controller.addResource(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItem(
                Matchers.containsString(request.toString())
        ));

        MatcherAssert.assertThat(response, Matchers.equalTo(expectedResponse));
    }
}
