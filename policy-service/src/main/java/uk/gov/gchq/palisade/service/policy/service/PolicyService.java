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
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
     * @param user the {@link User}| requesting the data
     * @param context the query time {@link Context} containing environmental variables
     *                such as why they want the data
     * @param resource the {@link Resource} being queried for access
     * @return an Optional {@link Resource} which is only present if the resource
     *         is accessible
     */
    Optional<Resource> canAccess(final User user, final Context context, final Resource resource);

    /**
     * This method gets the {@link Policy}s that apply to the resource
     * that the user has requested.
     *
     * @param resource a {@link Resource} to get policies for
     *
     * @return an Optional {@link Policy} if any policies exist for the resource
     */
    // FIXME: This cannot be typed as <T> Optional<Policy<T>> getPolicy(Resource resource)
    // There must be some input argument to specify T
    // Either through typing the class  --  <T> PolicyService<T>
    // Or supplying some sort of constructor factory  --  Producer<T>
    // Or passing the class as an argument  --  getPolicy(Resource, Class<? extends T>)
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
     * @param resource a {@link Resource} to set a policy for
     * @param policy the {@link Policy} to apply to this resource
     * @param <T> the record type for this resource
     *
     * @return the {@link Policy} that was added (may be different to what was requested)
     */
    <T> Policy<T> setResourcePolicy(Resource resource, Policy<T> policy);

    /**
     * This method allows for the setting of a policy to a resource type.
     *
     * @param type a resource type to apply a blanket policy to
     * @param policy the {@link Policy} to apply to this type
     * @param <T> the record type for this resource
     *
     * @return the {@link Policy} that was added (may be different to what was requested)
     */
    <T> Policy<T> setTypePolicy(String type, Policy<T> policy);
}
