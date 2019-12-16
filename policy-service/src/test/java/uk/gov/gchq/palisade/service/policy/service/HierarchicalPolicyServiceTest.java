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
package uk.gov.gchq.palisade.service.policy.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.HasSensitiveAuthRule;
import uk.gov.gchq.palisade.policy.HasTestingPurpose;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.StubResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.policy.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.Policy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class HierarchicalPolicyServiceTest {
    private static final SimpleCacheService cacheService = new SimpleCacheService().backingStore(new HashMapBackingStore());
    private final User user = new User().userId("testUser");
    private final FileResource fileResource1 = createTestFileResource(1);
    private final FileResource fileResource2 = createTestFileResource(2);
    private final SystemResource systemResource = createTestSystemResource();
    private final DirectoryResource directoryResource = createTestDirectoryResource();
    private HierarchicalPolicyService policyService;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;


    private static SystemResource createTestSystemResource() {
        return new SystemResource().id("File");
    }

    private static DirectoryResource createTestDirectoryResource() {
        DirectoryResource directoryResource = new DirectoryResource().id("File://temp");
        directoryResource.setParent(createTestSystemResource());
        return directoryResource;
    }

    private static FileResource createTestFileResource(final int i) {
        FileResource fileResource = new FileResource().id("File://temp/TestObj_00" + i + ".txt").type("TestObj" + i).serialisedFormat("txt");
        fileResource.setParent(createTestDirectoryResource());
        return fileResource;
    }

    @Before
    public void setup() {
        policyService = new HierarchicalPolicyService(cacheService);

        logger = (Logger) LoggerFactory.getLogger(HierarchicalPolicyService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        CompletableFuture<Boolean> request1 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(fileResource1)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Input is not null", new PassThroughRule<>())
                        .recordLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>()))
        );

        CompletableFuture<Boolean> request2 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(fileResource2)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Input is not null", new PassThroughRule<>())
                        .recordLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>()))
        );

        CompletableFuture<Boolean> request3 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(directoryResource)
                .policy(new Policy<>()
                        .owner(user)
                        .recordLevelRule("Does nothing", new PassThroughRule<>()))
        );

        CompletableFuture<Boolean> request4 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(systemResource)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Resource serialised format is txt", new IsTextResourceRule()))
        );

        CompletableFuture.allOf(request1, request2, request3, request4).join();
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
    public void getApplicableResourceLevelRules() {
        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(fileResource1, true, fileResource1.getType()).join();

        // Then
        assertTrue(optResult.isPresent());

        Rules<Object> result = optResult.get();
        assertEquals("Resource serialised format is txt, Input is not null", result.getMessage());
        assertEquals(2, result.getRules().keySet().size());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    @Test
    public void getApplicableRecordLevelRules() {
        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(fileResource1, false, fileResource1.getType()).join();

        // Then
        assertTrue(optResult.isPresent());
        Rules<Object> result = optResult.get();
        assertEquals("Does nothing, Check user has 'Sensitive' auth", result.getMessage());
        assertEquals(2, result.getRules().keySet().size());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    //should filter out resources where no policy is defined
    @Test
    public void shouldReturnEmptyResourceRulesOnNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(noPolicyStub, true, noPolicyStub.getType()).join();
        // Then
        assertFalse(optResult.isPresent());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    @Test
    public void shouldReturnEmptyRecordRulesOnNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(noPolicyStub, false, noPolicyStub.getType()).join();
        // Then
        assertFalse(optResult.isPresent());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    @Test
    public void canAccessIsValid() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // When
        CompletableFuture<CanAccessResponse> future = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Collections.singletonList(fileResource1))
                        .user(user)
                        .context(context));


        CanAccessResponse response = future.get();
        Collection<LeafResource> resources = response.getCanAccessResources();
        // Then
        assertEquals(1, resources.size());
        assertEquals(fileResource1, resources.iterator().next());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Determining access:"),
                Matchers.anyOf(
                        Matchers.containsString("Determining access: Context"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    @Test
    public void shouldRemoveResourcesWithNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        CompletableFuture<CanAccessResponse> future = policyService.canAccess(
                new CanAccessRequest()
                        .user(user)
                        .context(context)
                        .resources(Collections.singletonList(noPolicyStub)));

        CanAccessResponse response = future.join();
        // Then
        Assert.assertThat(response.getCanAccessResources(), is(CoreMatchers.equalTo(emptyList())));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Determining access"),
                Matchers.anyOf(
                        Matchers.containsString("no rules present"),
                        Matchers.containsString("No policy for StubResource"))
        ));
    }

    @Test
    public void getPolicy() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // When
        GetPolicyRequest getPolicyRequest = new GetPolicyRequest().user(user).context(context).resources(Collections.singletonList(fileResource1));
        getPolicyRequest.setOriginalRequestId(new RequestId().id("test getPolicy"));
        CompletableFuture<MultiPolicy> future = policyService.getPolicy(getPolicyRequest);
        MultiPolicy response = future.get();
        Map<LeafResource, Rules> ruleMap = response.getRuleMap();

        // Then
        assertEquals(1, ruleMap.size());
        assertEquals("Does nothing, Check user has 'Sensitive' auth", ruleMap.get(fileResource1).getMessage());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("getPolicy -"),
                Matchers.anyOf(
                        Matchers.containsString("adding resource:"))
        ));
    }

    @Test
    public void setPolicyForNewResource() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User testUser = new User().userId("testUser").auths("Sensitive");
        FileResource newResource = new FileResource().id("File://temp/TestObj_002.txt").type("TestObj").serialisedFormat("txt");
        newResource.setParent(createTestDirectoryResource());
        Policy newPolicy = new Policy()
                .owner(testUser)
                .resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());

        // When
        CompletableFuture<Boolean> future = policyService.setResourcePolicy(new SetResourcePolicyRequest().resource(newResource).policy(newPolicy));
        Boolean result = future.get();

        // Then
        assertTrue(result);

        // When
        CompletableFuture<CanAccessResponse> future2 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(newResource)).user(testUser).context(new Context().purpose("fun")));
        CanAccessResponse response2 = future2.get();
        Collection<LeafResource> resources2 = response2.getCanAccessResources();

        // Then
        assertEquals(0, resources2.size());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Setting resource policy"),
                Matchers.anyOf(
                        Matchers.containsString("Determining access:"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
    }

    @Test
    public void setPolicyForExistingResource() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User testUser = new User().userId("testUser").auths("Sensitive");
        Context testContext = new Context().purpose("testing");

        // When
        CompletableFuture<CanAccessResponse> future1 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(fileResource1)).user(testUser).context(testContext));
        CanAccessResponse response = future1.get();
        Collection<LeafResource> resources = response.getCanAccessResources();

        // Then
        assertEquals(1, resources.size());
        assertEquals(fileResource1, resources.iterator().next());

        // Given
        Policy newPolicy = new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());

        // When
        CompletableFuture<Boolean> future = policyService.setResourcePolicy(new SetResourcePolicyRequest().resource(fileResource1).policy(newPolicy));
        Boolean result = future.get();

        // Then
        assertTrue(result);

        // When
        CompletableFuture<CanAccessResponse> future2 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(fileResource1)).user(testUser).context(new Context().purpose("fun")));
        CanAccessResponse response2 = future2.get();
        Collection<LeafResource> resources2 = response2.getCanAccessResources();

        // Then
        assertEquals(0, resources2.size());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Setting resource policy "))
        ));
    }

    @Test
    public void setTypePolicy() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        final User testUser = new User().userId("testUser").auths("Sensitive");

        // Check before policy added
        final CompletableFuture<CanAccessResponse> canAccessBeforeResult = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Arrays.asList(fileResource1, fileResource2))
                        .user(testUser)
                        .context(new Context().purpose("fun"))
        );
        final Set<String> types = canAccessBeforeResult.get().getCanAccessResources().stream().map(LeafResource::getType).collect(Collectors.toSet());
        assertEquals(Sets.newHashSet("TestObj1", "TestObj2"), types);
        assertEquals(2, canAccessBeforeResult.get().getCanAccessResources().size());


        final Policy newPolicy = new Policy()
                .owner(testUser)
                .resourceLevelPredicateRule("Purpose is testing", (resource, user, context) -> context.getPurpose().equals("testing"));

        // When
        final CompletableFuture<Boolean> setPolicyResult = policyService.setTypePolicy(
                new SetTypePolicyRequest()
                        .type("TestObj2")
                        .policy(newPolicy)
        );

        // Then
        assertTrue(setPolicyResult.get());
        final CompletableFuture<CanAccessResponse> canAccessAfterResult = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Collections.singletonList(fileResource1))
                        .user(testUser)
                        .context(new Context().purpose("fun"))
        );
        assertEquals(1, canAccessAfterResult.get().getCanAccessResources().size());
        assertNotEquals("TestObj2", canAccessAfterResult.get().getCanAccessResources().iterator().next().getType());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting the applicable rules"),
                Matchers.anyOf(
                        Matchers.containsString("resource is an instance of ChildResource"),
                        Matchers.containsString("Getting the applicable rules: "))
        ));
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
package uk.gov.gchq.palisade.service.policy.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.policy.HasSensitiveAuthRule;
import uk.gov.gchq.palisade.policy.HasTestingPurpose;
import uk.gov.gchq.palisade.policy.IsTextResourceRule;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.StubResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.policy.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.Policy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class HierarchicalPolicyServiceTest {
    private static final SimpleCacheService cacheService = new SimpleCacheService().backingStore(new HashMapBackingStore());
    private final User user = new User().userId("testUser");
    private final FileResource fileResource1 = createTestFileResource(1);
    private final FileResource fileResource2 = createTestFileResource(2);
    private final SystemResource systemResource = createTestSystemResource();
    private final DirectoryResource directoryResource = createTestDirectoryResource();
    private HierarchicalPolicyService policyService;
    private TestLogger LOGGER = TestLoggerFactory.getTestLogger(HierarchicalPolicyService.class);

    private static SystemResource createTestSystemResource() {
        return new SystemResource().id("File");
    }

    private static DirectoryResource createTestDirectoryResource() {
        DirectoryResource directoryResource = new DirectoryResource().id("File://temp");
        directoryResource.setParent(createTestSystemResource());
        return directoryResource;
    }

    private static FileResource createTestFileResource(final int i) {
        FileResource fileResource = new FileResource().id("File://temp/TestObj_00" + i + ".txt").type("TestObj" + i).serialisedFormat("txt");
        fileResource.setParent(createTestDirectoryResource());
        return fileResource;
    }

    @Before
    public void setup() {
        policyService = new HierarchicalPolicyService(cacheService);
        LOGGER.clear();

        CompletableFuture<Boolean> request1 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(fileResource1)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Input is not null", new PassThroughRule<>())
                        .recordLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>()))
        );

        CompletableFuture<Boolean> request2 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(fileResource2)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Input is not null", new PassThroughRule<>())
                        .recordLevelRule("Check user has 'Sensitive' auth", new HasSensitiveAuthRule<>()))
        );

        CompletableFuture<Boolean> request3 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(directoryResource)
                .policy(new Policy<>()
                        .owner(user)
                        .recordLevelRule("Does nothing", new PassThroughRule<>()))
        );

        CompletableFuture<Boolean> request4 = policyService.setResourcePolicy(new SetResourcePolicyRequest()
                .resource(systemResource)
                .policy(new Policy<>()
                        .owner(user)
                        .resourceLevelRule("Resource serialised format is txt", new IsTextResourceRule()))
        );

        CompletableFuture.allOf(request1, request2, request3, request4).join();
    }

    @Test
    public void getApplicableResourceLevelRules() {
        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(fileResource1, true, fileResource1.getType()).join();

        // Then
        assertTrue(optResult.isPresent());

        Rules<Object> result = optResult.get();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        assertNotEquals(0, loggingEvents.size());
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));
        assertEquals("Resource serialised format is txt, Input is not null", result.getMessage());
        assertEquals(2, result.getRules().keySet().size());
    }

    @Test
    public void getApplicableRecordLevelRules() {
        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(fileResource1, false, fileResource1.getType()).join();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();

        // Then
        assertTrue(optResult.isPresent());
        Rules<Object> result = optResult.get();
        assertEquals("Does nothing, Check user has 'Sensitive' auth", result.getMessage());
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));
        assertEquals(2, result.getRules().keySet().size());
    }
    // should filter out resources where no policy is defined

    @Test
    public void shouldReturnEmptyResourceRulesOnNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(noPolicyStub, true, noPolicyStub.getType()).join();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();

        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));
        // Then
        assertFalse(optResult.isPresent());
    }

    @Test
    public void shouldReturnEmptyRecordRulesOnNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        Optional<Rules<Object>> optResult = policyService.getApplicableRules(noPolicyStub, false, noPolicyStub.getType()).join();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        assertFalse(optResult.isPresent());
    }

    @Test
    public void canAccessIsValid() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // When
        CompletableFuture<CanAccessResponse> future = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Collections.singletonList(fileResource1))
                        .user(user)
                        .context(context));


        CanAccessResponse response = future.get();
        Collection<LeafResource> resources = response.getCanAccessResources();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        assertEquals(1, resources.size());
        assertEquals(fileResource1, resources.iterator().next());
    }

    @Test
    public void shouldRemoveResourcesWithNoPolicy() {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // Set up a resource and parent with no policy attached
        SystemResource noPolicyParent = new SystemResource().id("nowhere");
        StubResource noPolicyStub = new StubResource();
        noPolicyStub.type("test").id("something");
        noPolicyStub.serialisedFormat("something");
        noPolicyStub.parent(noPolicyParent);

        // When
        CompletableFuture<CanAccessResponse> future = policyService.canAccess(
                new CanAccessRequest()
                        .user(user)
                        .context(context)
                        .resources(Collections.singletonList(noPolicyStub)));

        CanAccessResponse response = future.join();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        Assert.assertThat(response.getCanAccessResources(), is(CoreMatchers.equalTo(Collections.emptyList())));
    }

    @Test
    public void getPolicy() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User user = new User().userId("testUser").auths("Sensitive");
        Context context = new Context().purpose("testing");

        // When
        GetPolicyRequest getPolicyRequest = new GetPolicyRequest().user(user).context(context).resources(Collections.singletonList(fileResource1));
        getPolicyRequest.setOriginalRequestId(new RequestId().id("test getPolicy"));
        CompletableFuture<MultiPolicy> future = policyService.getPolicy(getPolicyRequest);
        MultiPolicy response = future.get();
        Map<LeafResource, Rules> ruleMap = response.getRuleMap();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        assertEquals(1, ruleMap.size());
        assertEquals("Does nothing, Check user has 'Sensitive' auth", ruleMap.get(fileResource1).getMessage());
    }

    @Test
    public void setPolicyForNewResource() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User testUser = new User().userId("testUser").auths("Sensitive");
        FileResource newResource = new FileResource().id("File://temp/TestObj_002.txt").type("TestObj").serialisedFormat("txt");
        newResource.setParent(createTestDirectoryResource());
        Policy newPolicy = new Policy()
                .owner(testUser)
                .resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());

        // When
        CompletableFuture<Boolean> future = policyService.setResourcePolicy(new SetResourcePolicyRequest().resource(newResource).policy(newPolicy));
        Boolean result = future.get();

        // Then
        assertTrue(result);

        // When
        CompletableFuture<CanAccessResponse> future2 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(newResource)).user(testUser).context(new Context().purpose("fun")));
        CanAccessResponse response2 = future2.get();
        Collection<LeafResource> resources2 = response2.getCanAccessResources();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        assertEquals(0, resources2.size());
    }

    @Test
    public void setPolicyForExistingResource() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        User testUser = new User().userId("testUser").auths("Sensitive");
        Context testContext = new Context().purpose("testing");

        // When
        CompletableFuture<CanAccessResponse> future1 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(fileResource1)).user(testUser).context(testContext));
        CanAccessResponse response = future1.get();
        Collection<LeafResource> resources = response.getCanAccessResources();

        // Then
        assertEquals(1, resources.size());
        assertEquals(fileResource1, resources.iterator().next());

        // Given
        Policy newPolicy = new Policy().owner(testUser).resourceLevelRule("Purpose is testing", new HasTestingPurpose<>());

        // When
        CompletableFuture<Boolean> future = policyService.setResourcePolicy(new SetResourcePolicyRequest().resource(fileResource1).policy(newPolicy));
        Boolean result = future.get();

        // Then
        assertTrue(result);

        // When
        CompletableFuture<CanAccessResponse> future2 = policyService.canAccess(new CanAccessRequest().resources(Collections.singletonList(fileResource1)).user(testUser).context(new Context().purpose("fun")));
        CanAccessResponse response2 = future2.get();
        Collection<LeafResource> resources2 = response2.getCanAccessResources();
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));

        // Then
        assertEquals(0, resources2.size());
    }

    @Test
    public void setTypePolicy() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        final User testUser = new User().userId("testUser").auths("Sensitive");

        // Check before policy added
        final CompletableFuture<CanAccessResponse> canAccessBeforeResult = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Arrays.asList(fileResource1, fileResource2))
                        .user(testUser)
                        .context(new Context().purpose("fun"))
        );
        final Set<String> types = canAccessBeforeResult.get().getCanAccessResources().stream().map(LeafResource::getType).collect(Collectors.toSet());
        assertEquals(Sets.newHashSet("TestObj1", "TestObj2"), types);
        assertEquals(2, canAccessBeforeResult.get().getCanAccessResources().size());


        final Policy newPolicy = new Policy()
                .owner(testUser)
                .resourceLevelPredicateRule("Purpose is testing", (resource, user, context) -> context.getPurpose().equals("testing"));

        // When
        final CompletableFuture<Boolean> setPolicyResult = policyService.setTypePolicy(
                new SetTypePolicyRequest()
                        .type("TestObj2")
                        .policy(newPolicy)
        );

        // Then
        assertTrue(setPolicyResult.get());
        final CompletableFuture<CanAccessResponse> canAccessAfterResult = policyService.canAccess(
                new CanAccessRequest()
                        .resources(Collections.singletonList(fileResource1))
                        .user(testUser)
                        .context(new Context().purpose("fun"))
        );
        assertEquals(1, canAccessAfterResult.get().getCanAccessResources().size());
        assertNotEquals("TestObj2", canAccessAfterResult.get().getCanAccessResources().iterator().next().getType());
        ImmutableList<LoggingEvent> loggingEvents = LOGGER.getLoggingEvents();
        List<Level> eventLevel = loggingEvents.stream().map(loggingEvent -> loggingEvent.getLevel()).collect(Collectors.toList());
        assertThat(eventLevel, not(hasItem(Level.ERROR)));
        assertThat(eventLevel, not(hasItem(Level.WARN)));
    }
}

>>>>>>> Stashed changes
