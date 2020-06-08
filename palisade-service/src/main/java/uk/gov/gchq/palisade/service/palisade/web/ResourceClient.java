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
package uk.gov.gchq.palisade.service.palisade.web;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import uk.gov.gchq.palisade.service.palisade.request.AddResourceRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesBySerialisedFormatRequest;
import uk.gov.gchq.palisade.service.palisade.request.GetResourcesByTypeRequest;

/**
 * The interface Resource client which uses Feign to resolve services called resource-service or falls back to values set in the relevant profiles yaml
 */
@FeignClient(name = "resource-service", url = "${web.client.resource-service}")
public interface ResourceClient {
    /**
     * Gets resources by id.
     *
     * @param request the request
     * @return the resources by id
     */
    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesById(@RequestBody final GetResourcesByIdRequest request);

    /**
     * Gets resources by resource.
     *
     * @param request the request
     * @return the resources by resource
     */
    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesByResource(@RequestBody final GetResourcesByResourceRequest request);

    /**
     * Gets resources by type.
     *
     * @param request the request
     * @return the resources by type
     */
    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesByType(@RequestBody final GetResourcesByTypeRequest request);

    /**
     * Gets resources by serialised format.
     *
     * @param request the request
     * @return the resources by serialised format
     */
    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesBySerialisedFormat(@RequestBody final GetResourcesBySerialisedFormatRequest request);

    /**
     * Add resource boolean.
     *
     * @param request the request
     * @return the boolean
     */
    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    Boolean addResource(@RequestBody final AddResourceRequest request);
}
