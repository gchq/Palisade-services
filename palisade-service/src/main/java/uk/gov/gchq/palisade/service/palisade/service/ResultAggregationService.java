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
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.palisade.policy.MultiPolicy;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;

import java.util.Map;
import java.util.concurrent.CompletionStage;

public class ResultAggregationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultAggregationService.class);

    public ResultAggregationService() {
    }

    public CompletionStage<DataRequestResponse> aggregateDataRequestResults(final User user, final Map<LeafResource, ConnectionDetail> resource, final MultiPolicy policy) {

        /*try {
            //remove any resources from the map that the policy doesn't contain details for -> user should not even be told about
            //resources they don't have permission to see
            Map<LeafResource, ConnectionDetail> filteredResources = removeDisallowedResources(resources.toCompletableFuture().join(), futureMultiPolicy.toCompletableFuture().join());

            PalisadeService.ensureRecordRulesAvailableFor(futureMultiPolicy.toCompletableFuture().join(), filteredResources.keySet());
            auditRegisterRequestComplete(request, (User) user, futureMultiPolicy.toCompletableFuture().join());
            cache(request, (User) user, requestId, futureMultiPolicy.toCompletableFuture().join(), filteredResources.size(), originalRequestId);
            final DataRequestResponse response = new DataRequestResponse().resources(filteredResources);
            response.setOriginalRequestId(originalRequestId);
            LOGGER.debug("Responding with: {}", response);
            return (CompletionStage<DataRequestResponse>) response;
        } catch (Exception ex) {
            LOGGER.error("Error handling: {}", ex.getMessage());
            auditRequestReceivedException(request, ex, PolicyService.class);
            throw new RuntimeException(ex); //rethrow the exception
        }*/

        return null;
    }
}
