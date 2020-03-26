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

package uk.gov.gchq.palisade.service.resource.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ResourceService;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(path = "/")
public class ResourceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

    private ResourceService service;

    public ResourceController(final ResourceService service) {
        this.service = service;
    }

    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesById(@RequestBody final GetResourcesByIdRequest request) {
        LOGGER.info("Invoking getResourceById: {}", request);
        Map<LeafResource, ConnectionDetail> response = getResourcesByIdRequest(request);
        LOGGER.info("Returning response {}", response);
        return response;
    }

    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(@RequestBody final GetResourcesByResourceRequest request) {
        LOGGER.info("Invoking getResourcesByResource: {}", request);
        Map<LeafResource, ConnectionDetail> response = getResourceByResourceRequest(request);
        LOGGER.info("Returning response {}", response);
        return response;
    }

    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesByType(@RequestBody final GetResourcesByTypeRequest request) {
        LOGGER.info("Invoking getResourceByType: {}", request);
        Map<LeafResource, ConnectionDetail> response = getResourceByTypeRequest(request);
        LOGGER.info("Returning response {}", response);
        return response;
    }

    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/json")
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(@RequestBody final GetResourcesBySerialisedFormatRequest request) {
        LOGGER.info("Invoking getResourcesBySerialisedFormatRequest: {}", request);
        Map<LeafResource, ConnectionDetail> response = getResourceBySerialisedFormat(request);
        LOGGER.info("Returning response {}", response);
        return response;
    }

    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    public Boolean addResource(@RequestBody final AddResourceRequest request) {
        LOGGER.info("Invoking addResource: {}", request);
        Boolean response = addResourceRequest(request);
        LOGGER.info("Returning response {}", response);
        return response;
    }

    private Map<LeafResource, ConnectionDetail> getResourcesByIdRequest(final GetResourcesByIdRequest request) {
        try {
            return service.getResourcesById(request).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while handling request {}: encountered {} {}", request, e.getClass(), e.getMessage());
            return null;
        }
    }

    private Map<LeafResource, ConnectionDetail> getResourceByResourceRequest(final GetResourcesByResourceRequest request) {
        try {
            return service.getResourcesByResource(request).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while handling request {}: encountered {} {}", request, e.getClass(), e.getMessage());
            return null;
        }
    }

    private Map<LeafResource, ConnectionDetail> getResourceByTypeRequest(final GetResourcesByTypeRequest request) {
        try {
            return service.getResourcesByType(request).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while handling request {}: encountered {} {}", request, e.getClass(), e.getMessage());
            return null;
        }
    }

    private Map<LeafResource, ConnectionDetail> getResourceBySerialisedFormat(final GetResourcesBySerialisedFormatRequest request) {
        try {
            return service.getResourcesBySerialisedFormat(request).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while handling request {}: encountered {} {}", request, e.getClass(), e.getMessage());
            return null;
        }
    }

    private Boolean addResourceRequest(final AddResourceRequest request) {
        try {
            return service.addResource(request).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while handling request {}: encountered {} {}", request, e.getClass(), e.getMessage());
            return null;
        }
    }
}
