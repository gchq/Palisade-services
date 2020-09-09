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

package uk.gov.gchq.palisade.service.policy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.request.Policy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimplePolicyService implements PolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePolicyService.class);

    private final Map<Resource, Policy> policyMap = new HashMap<>();

    @Override
    public Optional<Resource> canAccess(final User user, final Context context, final Resource resource) {
        if (policyMap.containsKey(resource)) {
            Collection<Rule<Resource>> resourceRules = policyMap.get(resource).getResourceRules().getRules().values();
            LOGGER.info("Resource {} has rules {}", resource, resourceRules);
            Optional<Resource> canAccess = resourceRules.stream()
                    .reduce(
                            Optional.of(resource), // Id: reduce over the requested resource
                            (acc, rule) -> acc.map(rsc -> rule.apply(rsc, user, context)), // Acc: apply each rule to the resource
                            (lPartial, rPartial) -> lPartial.flatMap(rsc -> rPartial)); // Comb: a resource is accessible if both partial results are accessible
            LOGGER.info("User {} with context {} has accessibility {}", user, context, canAccess);
            return canAccess;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Policy> getPolicy(final Resource resource) {
        return Optional.ofNullable(policyMap.get(resource));
    }

    @Override
    public <T extends Serializable> Policy<T> setResourcePolicy(final Resource resource, final Policy<T> policy) {
        policyMap.put(resource, policy);
        return policyMap.get(resource);
    }

    @Override
    public <T extends Serializable> Policy<T> setTypePolicy(final String type, final Policy<T> policy) {
        return null;
    }
}
