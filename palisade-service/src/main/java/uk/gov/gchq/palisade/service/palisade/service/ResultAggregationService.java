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

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class ResultAggregationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultAggregationService.class);
    private PersistenceLayer persistenceLayer;

    public ResultAggregationService(final PersistenceLayer persistenceLayer) {
        requireNonNull(persistenceLayer, "Persistence Layer");
        this.persistenceLayer = persistenceLayer;
    }

    public DataRequestResponse aggregateDataRequestResults(
            final RegisterDataRequest request,
            final CompletableFuture<User> user,
            final CompletableFuture<Set<LeafResource>> resource,
            final CompletableFuture<Map<LeafResource, Rules>> rules,
            final String token) {
        requireNonNull(request, "request");
        requireNonNull(user, "user");
        requireNonNull(resource, "resource");
        requireNonNull(rules, "rules");
        requireNonNull(token, "token");

        // remove any resources from the map that the rules doesn't contain details for -> user should not even be told about
        // resources they don't have permission to see
        Set<LeafResource> filteredResources = removeDisallowedResources(resource.join(), rules.join());

        final DataRequestConfig dataRequestConfig = new DataRequestConfig()
                .user(user.join())
                .context(request.getContext())
                .rules(rules.join());
        dataRequestConfig.setOriginalRequestId(request.getId());
        this.persistenceLayer.put(dataRequestConfig);

        final DataRequestResponse response = new DataRequestResponse()
                .resources(filteredResources)
                .token(token);
        response.setOriginalRequestId(request.getId());
        LOGGER.debug("Aggregated request with response: {}", response);

        return response;
    }

    /**
     * Removes all resource mappings in the {@code resources} that do not have defined rules in {@code rules}.
     *
     * @param resources the resources to modify
     * @param rules     the rules for all resources
     * @return the {@code resources} set after filtering
     */
    private Set<LeafResource> removeDisallowedResources(final Set<LeafResource> resources, final Map<LeafResource, Rules> rules) {
        LOGGER.debug("removeDisallowedResources({}, {})", resources, rules);

        resources.retainAll(rules.keySet());

        LOGGER.debug("Allowed resources: {}", resources);
        return resources;
    }
}
