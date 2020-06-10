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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;

@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private PolicyClient policyClient = Mockito.mock(PolicyClient.class);
    private PolicyService policyService;
    private Map<LeafResource, Rules> rules = new HashMap<>();
    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        logger = (Logger) LoggerFactory.getLogger(PolicyService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        policyService = new PolicyService(policyClient, executor);
        FileResource resource = new FileResource().id("/path/to/bob_file.txt");
        Rules rule = new Rules().rule("Rule1", new PassThroughRule());
        rules.put(resource, rule);

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
    public void infoOnGetPolicyRequest() {
        // Given
        GetPolicyRequest request = Mockito.mock(GetPolicyRequest.class);
        Map<LeafResource, Rules> response = Mockito.mock(Map.class);
        Mockito.when(policyClient.getPolicySync(Mockito.eq(request))).thenReturn(response);

        // When
        policyService.getPolicy(request).join();

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.anyOf(
                        Matchers.containsString(response.toString()),
                        Matchers.containsString("Not completed"))
        ));
    }

    @Test
    public void getPolicyReturnsPolicy() {
        //Given
        GetPolicyRequest request = new GetPolicyRequest().user(new User().userId("Bob")).context(new Context().purpose("Testing"));
        Map<LeafResource, Rules> response = Mockito.mock(Map.class);
        Mockito.when(policyClient.getPolicySync(Mockito.eq(request))).thenReturn(response);

        //When
        CompletableFuture<Map<LeafResource, Rules>> actual = policyService.getPolicy(request);

        //Then
        MatcherAssert.assertThat(response, equalTo(actual.join()));
    }

}
