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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.policy.request.CanAccessRequest;
import uk.gov.gchq.palisade.service.policy.request.CanAccessResponse;
import uk.gov.gchq.palisade.service.policy.request.GetPolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.MultiPolicy;
import uk.gov.gchq.palisade.service.policy.request.Policy;
import uk.gov.gchq.palisade.service.policy.request.SetResourcePolicyRequest;
import uk.gov.gchq.palisade.service.policy.request.SetTypePolicyRequest;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The core API for the policy service.
 * The responsibilities of the policy service is to provide the set of rules
 * (filters or transformations) that need to be applied to each resource that
 * has been requested, based the user and context.
 * Note that a resource could be a file, stream, directory or even the system
 * resource (policies added to the system resource would be applied globally).
 */
public interface PolicyService extends Service {

    /**
     * This method is used to find out if the given user is allowed to access
     * the resource given their purpose. This is where any resource level
     * access controls are enforced.
     *
     * @param request a {@link CanAccessRequest} containing the user requesting
     *                the data, the query time context containing environmental
     *                variables such as why they want the data and
     *                collection of resource's containing that data.
     * @return a {@link CanAccessResponse} which contains a collection of the
     * resources that the user is allowed access too.
     */
    Optional<Resource> canAccess(final User user, final Context context, final Resource resource);

    /**
     * This method gets the record level {@link Policy}'s that apply to the list
     * of resources that the user has requested access too.
     *
     * @param request a {@link GetPolicyRequest} containing the user requesting
     *                the data, the query time context containing environmental
     *                variables such as why they want the data and
     *                list of the resources the user wants access too.
     * @return a {@link MultiPolicy} containing the mapping of resource to {@link Policy}
     */
    Optional<Policy> getPolicy(Resource resource);

    default Map<Resource, Policy> getPolicy(final Collection<Resource> resources) {
        return resources.stream()
                .map(resource -> getPolicy(resource).map(policy -> new SimpleEntry<>(resource, policy)))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    /**
     * This method allows for the setting of a policy to a resource.
     *
     * @param request a {@link SetResourcePolicyRequest} containing the
     *                resource and the policy to set on that resource.
     * @return a {@link CompletableFuture} {@link Boolean} which is true if
     * the policy was successfully set.
     */
    Policy setResourcePolicy(Resource resource, Policy policy);

    /**
     * This method allows for the setting of a policy to a resource type.
     *
     * @param request a {@link SetTypePolicyRequest} containing the
     *                resource type and the policy to set on that resource.
     * @return a {@link CompletableFuture} {@link Boolean} which is true if
     * the policy was successfully set.
     */
    Policy setTypePolicy(String type, Policy policy);

}
