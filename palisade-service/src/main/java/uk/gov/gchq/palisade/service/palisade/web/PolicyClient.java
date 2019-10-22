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
package uk.gov.gchq.palisade.service.palisade.web;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.palisade.request.GetPolicyRequest;

import java.util.concurrent.CompletableFuture;

@FeignClient(name = "policy-service", url = "${web.client.policy-service}")
public interface PolicyClient {

    @PostMapping(path = "/getPolicy", consumes = "application/json", produces = "application/json")
    CompletableFuture<MultiPolicy> getPolicy(final GetPolicyRequest request);

}
