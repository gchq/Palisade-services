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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.resource.request.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A implementation of the ResourceService.
 * <p>
 * This service is for the retrieval of Resources only. Resources cannot be added via this service, they should be added
 * through the actual real filing system.
 *
 * @see SimpleResourceService
 */

public class SimpleResourceService implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceService.class);

    static final String ERROR_ADD_RESOURCE = "AddResource is not supported by the Resource Service, resources should be added/created via regular file system behaviour.";

    private final ResourceService service;
    private final Executor executor;

    private List<ConnectionDetail> dataServices = new ArrayList<>();
    private String filename;

    public SimpleResourceService(final ResourceService service, final Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByResource(GetResourcesByResourceRequest request) {
        final RequestId originalRequestId = request.getOriginalRequestId();
        LOGGER.debug("Invoking getResourcesByResource request: {}", request);
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(GetResourcesByIdRequest request) {
        final RequestId originalRequestId = request.getOriginalRequestId();
        LOGGER.debug("Invoking getResourcesById request: {}", request);
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByType(GetResourcesByTypeRequest request) {
        final RequestId originalRequestId = request.getOriginalRequestId();
        LOGGER.debug("Invoking getResourcesByType request: {}", request);
        return null;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesBySerialisedFormat(GetResourcesBySerialisedFormatRequest request) {
        final RequestId originalRequestId = request.getOriginalRequestId();
        LOGGER.debug("Invoking getResourcesBySerialisedFormat request: {}", request);
        return null;
    }

    @Override
    public CompletableFuture<Boolean> addResource(AddResourceRequest request) {
        throw new UnsupportedOperationException(ERROR_ADD_RESOURCE);
    }
}
