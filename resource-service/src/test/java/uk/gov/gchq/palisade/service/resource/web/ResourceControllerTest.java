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
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.request.Request;
import uk.gov.gchq.palisade.service.resource.impl.MockResourceService;
import uk.gov.gchq.palisade.service.resource.service.ResourceServiceCachingProxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class ResourceControllerTest {
    private static LeafResource resource = new FileResource().id("/path/test_file.txt").type("test").serialisedFormat("txt");
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private ResourceController controller;
    private MockResourceService resourceService;

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ResourceController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        resourceService = new MockResourceService();
        controller = new ResourceController(new ResourceServiceCachingProxy(resourceService));
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @DataPoints("GetRequests")
    public static List<Request> requests = Arrays.asList(
            new GetResourcesByIdRequest().resourceId(resource.getId()),
            new GetResourcesByResourceRequest().resource(resource),
            new GetResourcesBySerialisedFormatRequest().serialisedFormat(resource.getSerialisedFormat()),
            new GetResourcesByTypeRequest().type(resource.getType()));

    @DataPoints("GetErrorRequests")
    public static List<Request> errorRequests = Arrays.asList(
            new GetResourcesByIdRequest(),
            new GetResourcesByResourceRequest(),
            new GetResourcesBySerialisedFormatRequest(),
            new GetResourcesByTypeRequest());

    @DataPoint("AddRequest")
    public static AddResourceRequest addRequest = new AddResourceRequest().resource(resource);

    @DataPoint("AddErrorRequest")
    public static AddResourceRequest addErrorRequest = new AddResourceRequest();

    private Map<Class<? extends Request>, Function<Request, Map<LeafResource, ConnectionDetail>>> requestMethods = new HashMap<>();
    {
        requestMethods.put(GetResourcesByIdRequest.class, request -> {
            request.originalRequestId(new RequestId().id("originalId"));
            return controller.getResourcesById((GetResourcesByIdRequest) request);
        });
        requestMethods.put(GetResourcesByResourceRequest.class, request -> controller.getResourcesByResource((GetResourcesByResourceRequest) request));
        requestMethods.put(GetResourcesBySerialisedFormatRequest.class, request -> controller.getResourcesBySerialisedFormat((GetResourcesBySerialisedFormatRequest) request));
        requestMethods.put(GetResourcesByTypeRequest.class, request -> controller.getResourcesByType((GetResourcesByTypeRequest) request));
    }

    @Theory
    public void infoOnGetResourceRequest(@FromDataPoints("GetRequests") Request request) {
        // Given
        Function<Request, Map<LeafResource, ConnectionDetail>> method = requestMethods.get(request.getClass());
        Map<LeafResource, ConnectionDetail> expectedResponse = resourceService.getMockingMap().get(request.getClass());

        // When
        Map<LeafResource, ConnectionDetail> response = method.apply(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.containsString(response.toString())
        ));

        MatcherAssert.assertThat(response, Matchers.equalTo(expectedResponse));
    }

    @Theory
    public void errorOnRequestException(@FromDataPoints("GetErrorRequests") Request request) {
        // Given
        Function<Request, Map<LeafResource, ConnectionDetail>> method = requestMethods.get(request.getClass());
        expectedException.expect(NullPointerException.class);

        // When
        request.setOriginalRequestId(new RequestId().id("originalId"));
        method.apply(request);
    }

    @Theory
    public void infoOnAddResourceRequest(@FromDataPoints("AddRequest") AddResourceRequest request) {
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

    @Theory
    public void errorOnAddException(@FromDataPoints("AddErrorRequest") AddResourceRequest request) {

        // Given
        expectedException.expect(NullPointerException.class);

        // When
        controller.addResource(request);
    }
}
