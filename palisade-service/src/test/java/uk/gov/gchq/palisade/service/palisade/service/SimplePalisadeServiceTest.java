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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ServiceState;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.repository.BackingStore;
import uk.gov.gchq.palisade.service.palisade.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.palisade.request.GetDataRequestConfig;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.palisade.web.AuditClient;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class SimplePalisadeServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeServiceTest.class);

    private final DataRequestConfig expectedConfig = new DataRequestConfig();
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private AuditService auditService;
    private SimpleCacheService cacheService;
    private PolicyService policyService;
    private ResourceService resourceService;
    private UserService userService;
    private SimplePalisadeService service;
    private ServiceState serviceState = new ServiceState();

    @Before
    public void setup() {
        setupCacheService();
        mockOtherServices();
        service = new SimplePalisadeService(auditService, userService, policyService, resourceService, cacheService, applicationConfig.getAsyncExecutor());
        LOGGER.info("Simple Palisade Service created: {}", service);
        createExpectedDataConfig();
    }

    @Test
    public void getDataRequestConfigTest() throws Exception{
        LOGGER.info("Expected config: {}", expectedConfig);
        //Given
        GetDataRequestConfig requestConfig = new GetDataRequestConfig();
        requestConfig.requestId(new RequestId().id("requestId"));
        requestConfig.resource(new FileResource().id("resourceId"));
        LOGGER.info("Get Data Request Config: {}", requestConfig);

        DataRequestConfig actualConfig;

        //When
        actualConfig = service.getDataRequestConfig(requestConfig).get();

        //Then
        assertEquals(expectedConfig, actualConfig);
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
    }

    private void setupCacheService() {
        final BackingStore store = Mockito.mock(BackingStore.class);
        LOGGER.debug("Store Class: {}", store._getClass());
        serviceState.put("cache.svc.store", null);
        serviceState.put("cache.svc.max.ttl", Duration.of(5, ChronoUnit.MINUTES).toString());
        cacheService = new SimpleCacheService();
        cacheService.recordCurrentConfigTo(serviceState);
        cacheService.backingStore(store);
    }
}
