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

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.palisade.web.PolicyClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class PolicyService implements Service {

    private final PolicyClient client;
    private final Executor executor;

    public PolicyService(final PolicyClient policyClient, final Executor executor) {
        this.client = policyClient;
        this.executor = executor;
    }

    public CompletionStage<MultiPolicy> getPolicy(final GetPolicyRequest request) {
        return CompletableFuture.supplyAsync(() -> this.client.getPolicy(request), this.executor);
    }
}
