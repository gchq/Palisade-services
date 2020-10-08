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
package uk.gov.gchq.palisade.component.policy;


import feign.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import uk.gov.gchq.palisade.component.policy.config.PolicyTestConfiguration;
import uk.gov.gchq.palisade.component.policy.web.PolicyClient;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.policy.PolicyApplication;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;
import uk.gov.gchq.palisade.service.policy.web.PolicyController;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PolicyTestConfiguration.class)
@EnableFeignClients
@SpringBootTest(classes = {PolicyApplication.class, PolicyController.class, PolicyConfiguration.class}, webEnvironment = WebEnvironment.DEFINED_PORT)
public class PolicyComponentTest extends PolicyTestCommon {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyComponentTest.class);

    @Autowired
    private Map<String, PolicyService> serviceMap;

    @Autowired
    private PolicyClient policyClient;

    @Test
    public void testContextLoads() {
        assertThat(serviceMap).isNotNull();
        assertThat(serviceMap).isNotEmpty();
    }

    @Test
    public void testIsUp() {
        Response health = policyClient.getActuatorHealth();
        assertThat(health.status()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void testComponentTest() {
        // Given there are resources and policies to be added
        Collection<LeafResource> resources = Collections.singleton(NEW_FILE);

        // When a resource is added
        SetResourcePolicyRequest addRequest = new SetResourcePolicyRequest().resource(NEW_FILE).policy(PASS_THROUGH_POLICY);
        policyClient.setResourcePolicyAsync(addRequest);

        // Given it is accessible
        CanAccessRequest accessRequest = new CanAccessRequest().user(USER).resources(resources).context(CONTEXT);
        CanAccessResponse accessResponse = policyClient.canAccess(accessRequest);
        for (LeafResource resource : resources) {
            assertThat(accessResponse.getCanAccessResources()).contains(resource);
        }

        // When the policies on the resource are requested
        GetPolicyRequest getRequest = new GetPolicyRequest().user(USER).resources(resources).context(CONTEXT);
        Map<LeafResource, Rules> getResponse = policyClient.getPolicySync(getRequest);
        LOGGER.info("Response: {}", getResponse);

        // Then the policy just added is found on the resource
        assertThat(getResponse.get(NEW_FILE)).isEqualTo(PASS_THROUGH_POLICY.getRecordRules());
    }
}
