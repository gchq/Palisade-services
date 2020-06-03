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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class PolicyService implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyClient client;

    private final Executor executor;

    public PolicyService(final PolicyClient policyClient, final Executor executor) {
        this.client = policyClient;
        this.executor = executor;
    }

    public CompletableFuture<Map<LeafResource, Rules>> getPolicy(final GetPolicyRequest request) {
        LOGGER.debug("Getting policy from policy service: {}", request);

        CompletionStage<Map<LeafResource, Rules>> policy;
        try {
            LOGGER.info("Policy request: {}", request);
            policy = CompletableFuture.supplyAsync(() -> {
                Map<LeafResource, Rules> response = client.getPolicySync(request);
                LOGGER.info("Got policy: {}", response);
                return response;
            }, this.executor);
        } catch (Exception ex) {
            LOGGER.error("Failed to get policy: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }

        return policy.toCompletableFuture();
    }

}
