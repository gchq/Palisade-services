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

package uk.gov.gchq.palisade.service.resource.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.resource.request.AddCacheRequest;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class HadoopResourceConfigurationService extends HadoopResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopResourceConfigurationService.class);

    private final CacheService cacheService;

    public HadoopResourceConfigurationService(final CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public CompletableFuture<Boolean> addResource(final AddResourceRequest addResourceRequest) {
        LOGGER.debug("Adding Resource : {}", addResourceRequest);
        LOGGER.info("Adding Resource: {}", addResourceRequest.getOriginalRequestId().getId());
        requireNonNull(addResourceRequest, "Request cannot be empty");
        requireNonNull(addResourceRequest.getResource(), "Request Resource cannot be empty");
        requireNonNull(addResourceRequest.getConnectionDetail(), "Request Connection cannot be empty");

        AddCacheRequest<ConnectionDetail> addCacheRequest = new AddCacheRequest<ConnectionDetail>()
                .service(this.getClass())
                .key(addResourceRequest.getResource().toString())
                .value(addResourceRequest.getConnectionDetail());
        cacheService.add(addCacheRequest).join();
        LOGGER.debug("Resource added: {}", addCacheRequest);
        LOGGER.info("Resource added: {}, {}", addCacheRequest.getKey(), addCacheRequest.getValue());

        return CompletableFuture.completedFuture(true);
    }
}
