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

package uk.gov.gchq.palisade.service.palisade.service;

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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private ResourceClient resourceClient = Mockito.mock(ResourceClient.class);
    private ResourceService resourceService;
    private Set<LeafResource> resources = new HashSet<>();
    private ExecutorService executor;
    private Supplier<URI> uriSupplier = () -> {
        try {
            return new URI("resource-service");
        } catch (Exception e) {
            return null;
        }
    };
    private FileResource resource = new FileResource().id("/path/to/bob_file.txt");
    private ConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("data-service");

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        logger = (Logger) LoggerFactory.getLogger(ResourceService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        resources.add(resource.connectionDetail(connectionDetail));
        resourceService = Mockito.spy(new ResourceService(resourceClient, uriSupplier, null, executor));
        Mockito.doReturn(resources).when(resourceService).getResourcesFromFeignResponse(Mockito.any());
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
    public void debugOnGetResourcesRequest() {
        // Given
        GetResourcesByIdRequest request = new GetResourcesByIdRequest().resourceId("/path/to/bob_file.txt");
        request.setOriginalRequestId(new RequestId().id("Original ID"));

        // When
        resourceService.getResourcesById(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.getOriginalRequestId().getId()),
                Matchers.containsString(uriSupplier.get().toString())
        ));

    }

    @Test
    public void getResourceByIdReturnsMappedResources() {
        //Given

        //When
        GetResourcesByIdRequest request = new GetResourcesByIdRequest().resourceId("/path/to/bob_file.txt");
        request.setOriginalRequestId(new RequestId().id("Original ID"));
        CompletableFuture<Set<LeafResource>> actual = resourceService.getResourcesById(request);

        //Then
        assertEquals(resources, actual.join());
    }

}
