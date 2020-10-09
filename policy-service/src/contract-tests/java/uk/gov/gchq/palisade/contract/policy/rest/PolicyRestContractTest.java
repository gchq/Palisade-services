/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.contract.policy.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.contract.policy.PolicyTestCommon;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.PolicyApplication;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PolicyApplication.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {PolicyTestConfiguration.class})
@ActiveProfiles("caffeine")
class PolicyRestContractTest extends PolicyTestCommon {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testIsUp() {
        ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    public static class LeafResourceRulesMap extends HashMap<LeafResource, Rules> {
    }

    @Test
    void testRestEndpoint() {
        // Given there are resources and policies to be added
        Collection<LeafResource> resources = Collections.singleton(NEW_FILE);

        // When a resource is added
        SetResourcePolicyRequest addRequest = new SetResourcePolicyRequest().resource(NEW_FILE).policy(PASS_THROUGH_POLICY);
        restTemplate.put("/setResourcePolicyAsync", addRequest);

        // Given it is accessible
        CanAccessRequest accessRequest = new CanAccessRequest().user(USER).resources(resources).context(CONTEXT);
        CanAccessResponse accessResponse = restTemplate.postForObject("/canAccess", accessRequest, CanAccessResponse.class);
        for (LeafResource resource : resources) {
            assertThat(accessResponse.getCanAccessResources()).contains(resource);
        }

        // When the policies on the resource are requested
        GetPolicyRequest getRequest = new GetPolicyRequest().user(USER).resources(resources).context(CONTEXT);
        LeafResourceRulesMap getResponse = restTemplate.postForObject("/getPolicySync", getRequest, LeafResourceRulesMap.class);

        // Then the policy just added is found on the resource
        assertThat(getResponse).containsEntry(NEW_FILE, PASS_THROUGH_POLICY.getRecordRules());
    }
}
