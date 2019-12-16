<<<<<<< Updated upstream
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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.policy.Policy;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private PolicyClient policyClient = Mockito.mock(PolicyClient.class);
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private PolicyService policyService;
    private MultiPolicy multiPolicy;
    private User testUser = new User().userId("Bob");
    private Map<LeafResource, Policy> policies = new HashMap<>();
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
        Policy policy = new Policy().owner(testUser);
        policies.put(resource, policy);
        multiPolicy = new MultiPolicy().policies(policies);
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
    public void infoOnGetPolicyRequest() {
        // Given
        GetPolicyRequest request = Mockito.mock(GetPolicyRequest.class);
        MultiPolicy response = Mockito.mock(MultiPolicy.class);
        Mockito.when(policyClient.getPolicy(request)).thenReturn(response);

        // When
        policyService.getPolicy(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.anyOf(
                        Matchers.containsString(response.toString()),
                        Matchers.containsString("Not completed"))
        ));
    }

    @Test
    public void getPolicyReturnsPolicy() {
        //Given
        when(policyClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(multiPolicy);

        //When
        GetPolicyRequest request = new GetPolicyRequest().user(new User().userId("Bob")).context(new Context().purpose("Testing"));
        CompletableFuture<MultiPolicy> actual = policyService.getPolicy(request);

        //Then
        assertEquals(multiPolicy, actual.join());
    }

}
=======
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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.policy.Policy;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private PolicyClient policyClient = Mockito.mock(PolicyClient.class);
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private PolicyService policyService;
    private MultiPolicy multiPolicy;
    private User testUser = new User().userId("Bob");
    private Map<LeafResource, Policy> policies = new HashMap<>();

    @Before
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PolicyService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        policyService = new PolicyService(policyClient, applicationConfig.getAsyncExecutor());
        FileResource resource = new FileResource().id("/path/to/bob_file.txt");
        Policy policy = new Policy().owner(testUser);
        policies.put(resource, policy);
        multiPolicy = new MultiPolicy().policies(policies);
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
    public void infoOnGetPolicyRequest() {
        // Given
        GetPolicyRequest request = Mockito.mock(GetPolicyRequest.class);
        MultiPolicy response = Mockito.mock(MultiPolicy.class);
        Mockito.when(policyClient.getPolicy(request)).thenReturn(response);

        // When
        policyService.getPolicy(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.toString()),
                Matchers.anyOf(
                        Matchers.containsString(response.toString()),
                        Matchers.containsString("Not completed"))
        ));
    }

    @Test
    public void getPolicyReturnsPolicy() {
        //Given
        when(policyClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(multiPolicy);

        //When
        GetPolicyRequest request = new GetPolicyRequest().user(new User().userId("Bob")).context(new Context().purpose("Testing"));
        CompletableFuture<MultiPolicy> actual = policyService.getPolicy(request);

        //Then
        assertEquals(multiPolicy, actual.join());
    }

}
>>>>>>> Stashed changes
