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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.policy.request.Policy;

import java.util.Optional;

public class NullPolicyService implements PolicyService {
    @Override
    public Optional<Resource> canAccess(final User user, final Context context, final Resource resource) {
        // By default, all resources can be accessed (this may be overruled by hierarchy)
        return Optional.of(resource);
    }

    @Override
    public Optional<Policy> getPolicy(final Resource resource) {
        // No policies ever exist, so none are returned when requested (actual returns may come from cache)
        return Optional.empty();
    }

    @Override
    public <T> Policy<T> setResourcePolicy(final Resource resource, final Policy<T> policy) {
        // Policies cannot be stored, but pretend that they are (they will be cached)
        return policy;
    }

    @Override
    public <T> Policy<T> setTypePolicy(final String type, final Policy<T> policy) {
        // Policies cannot be stored, but pretend that they are (they will be cached)
        throw new RuntimeException(String.format("%s::setTypePolicy not implemented", this.getClass()));
    }
}
