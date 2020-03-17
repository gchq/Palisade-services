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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.AuditRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class SimplePalisadeServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeServiceTest.class);

    private final DataRequestConfig expectedConfig = new DataRequestConfig();
    private ResultAggregationService aggregationService;
    private AuditService auditService;
    private PersistenceLayer persistenceLayer;
    private PolicyService policyService;
    private ResourceService resourceService;
    private UserService userService;
    private SimplePalisadeService service;
    private CompletableFuture<DataRequestResponse> futureResponse = new CompletableFuture<>();
    private DataRequestResponse expectedResponse = new DataRequestResponse();
    private RegisterDataRequest dataRequest = new RegisterDataRequest();
    private DataRequestConfig dataRequestConfig = new DataRequestConfig();
    private RequestId originalRequestId = new RequestId().id("Bob");

    private User user;
    private Map<LeafResource, ConnectionDetail> resources = new HashMap<>();
    private Map<LeafResource, Rules> rules = new HashMap<>();
    private ExecutorService executor;

    @Before
    public void setup() {
        executor = Executors.newSingleThreadExecutor();
        mockOtherServices();
        service = new SimplePalisadeService(auditService, userService, policyService, resourceService, persistenceLayer, executor, aggregationService);
        LOGGER.info("Simple Palisade Service created: {}", service);
        createExpectedDataConfig();
        user = new User().userId("Bob").roles("Role1", "Role2").auths("Auth1", "Auth2");

        FileResource resource = new FileResource().id("/path/to/new/bob_file.txt").type("bob").serialisedFormat("txt")
                .parent(new DirectoryResource().id("/path/to/new/")
                        .parent(new DirectoryResource().id("/path/to/")
                                .parent(new DirectoryResource().id("/path/")
                                        .parent(new SystemResource().id("/")))));
        ConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("data-service");
        resources.put(resource, connectionDetail);

        Rules rule = new Rules().rule("Rule1", new PassThroughRule());
        rules.put(resource, rule);

        dataRequest.userId(new UserId().id("Bob")).context(new Context().purpose("Testing")).resourceId("/path/to/new/bob_file.txt");
        dataRequestConfig.user(user).context(dataRequest.getContext()).rules(rules);
        dataRequestConfig.setOriginalRequestId(originalRequestId);
        expectedResponse.resources(resources);
        expectedResponse.originalRequestId(originalRequestId);
        futureResponse.complete(expectedResponse);
    }

    @Test
    public void registerDataRequestTest() {

        //Given
        CompletableFuture<User> futureUser = new CompletableFuture<>();
        futureUser.complete(user);
        CompletableFuture<Map<LeafResource, ConnectionDetail>> futureResource = new CompletableFuture<>();
        futureResource.complete(resources);
        CompletableFuture<Map<LeafResource, Rules>> futurePolicy = new CompletableFuture<>();
        futurePolicy.complete(rules);

        RegisterDataRequest request = new RegisterDataRequest()
                .userId(new UserId().id("Bob"))
                .context(new Context().purpose("Testing"))
                .resourceId("/path/to/new/bob_file.txt");

        when(auditService.audit(any(AuditRequest.class))).thenReturn(true);
        when(userService.getUser(any(GetUserRequest.class))).thenReturn(futureUser);
        when(resourceService.getResourcesById(any(GetResourcesByIdRequest.class))).thenReturn(futureResource);
        when(policyService.getPolicy(any(GetPolicyRequest.class))).thenReturn(futurePolicy);
        when(aggregationService.aggregateDataRequestResults(any(RegisterDataRequest.class), any(User.class), anyMap(), anyMap(), any(RequestId.class), any(RequestId.class)))
                .thenReturn(futureResponse);

        //When
        CompletableFuture<DataRequestResponse> response = service.registerDataRequest(request);
        DataRequestResponse actualResponse = response.join();
        actualResponse.originalRequestId(request.getId());

        //Then
        assertEquals(expectedResponse.getResources(), actualResponse.getResources());
    }

    @Test(expected = CompletionException.class)
    public void getDataRequestConfigFromEmptyCacheTest() {
        LOGGER.info("Expected config: {}", expectedConfig);
        //Given
        GetDataRequestConfig requestConfig = new GetDataRequestConfig();
        requestConfig.token(new RequestId().id("requestId"));
        requestConfig.resource(new FileResource().id("resourceId"));
        requestConfig.setOriginalRequestId(new RequestId().id("original-request-id"));
        LOGGER.info("Get Data Request Config: {}", requestConfig);
        when(persistenceLayer.getAsync(anyString())).thenReturn(CompletableFuture.failedFuture(new CompletionException(new RuntimeException())));

        //When
        CompletableFuture<DataRequestConfig> cacheConfig = service.getDataRequestConfig(requestConfig);
        cacheConfig.toCompletableFuture().join();
    }

    private void createExpectedDataConfig() {
        User user = new User();
        Context context = new Context();
        Map<LeafResource, Rules> ruleMap = new HashMap<>();
        FileResource resource = new FileResource();
        Rules rules = new Rules();
        Set<String> roles = new HashSet<>();
        Set<String> auth = new HashSet<>();

        user.setUserId(new UserId().id("userId"));
        roles.add("roles");
        user.setRoles(roles);
        auth.add("auth");
        user.setAuths(auth);
        context.purpose("purpose");
        ruleMap.put(resource, rules);
        expectedConfig.setUser(user);
        expectedConfig.setContext(context);
        expectedConfig.setRules(ruleMap);
    }

    private void mockOtherServices() {
        auditService = Mockito.mock(AuditService.class);
        policyService = Mockito.mock(PolicyService.class);
        resourceService = Mockito.mock(ResourceService.class);
        userService = Mockito.mock(UserService.class);
        aggregationService = Mockito.mock(ResultAggregationService.class);
        persistenceLayer = Mockito.mock(PersistenceLayer.class);
    }

}
