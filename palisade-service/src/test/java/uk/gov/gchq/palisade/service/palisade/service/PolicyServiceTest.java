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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.policy.Policy;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class PolicyServiceTest {

    private PolicyClient policyClient = Mockito.mock(PolicyClient.class);
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private PolicyService policyService;
    private MultiPolicy multiPolicy;
    private User testUser = new User().userId("Bob");
    private Map<LeafResource, Policy> policies = new HashMap<>();

    @Before
    public void setup() {
        policyService = new PolicyService(policyClient, applicationConfig.getAsyncExecutor());
        FileResource resource = new FileResource().id("/path/to/bob_file.txt");
        Policy policy = new Policy().owner(testUser);
        policies.put(resource, policy);
        multiPolicy = new MultiPolicy().policies(policies);
    }

    @Test
    public void getPolicyReturnsPolicy() {
        //Given
        CompletableFuture<MultiPolicy> futurePolicy = new CompletableFuture<>();
        futurePolicy.complete(multiPolicy);
        when(policyClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(futurePolicy);

        //When
        GetPolicyRequest request = new GetPolicyRequest().user(new User().userId("Bob")).context(new Context().purpose("Testing"));
        CompletableFuture<MultiPolicy> actual = policyService.getPolicy(request);

        //Then
        assertEquals(multiPolicy, actual.join());
    }

    @Test(expected = RuntimeException.class)
    public void getPolicyReturnsError() {

        //Given
        when(policyClient.getPolicy(any(GetPolicyRequest.class))).thenThrow(new RuntimeException());

        //When
        GetPolicyRequest request = new GetPolicyRequest().user(testUser);
        CompletableFuture<MultiPolicy> actual = policyService.getPolicy(request);
    }

}
