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
 * The interface Resource client which uses Feign and uses services urls if provided, otherwise discovery by name with eureka
 */
@FeignClient(name = "resource-service", url = "${web.client.resource-service}")
public interface ResourceClient {
    /**
     * Post rest request to the Resource Service and returns a response containing resources and connection details by resourceID
     *
     * @param request the request
     * @return the resources by id
     */
    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesById(@RequestBody final GetResourcesByIdRequest request);

    /**
     * Post rest request to the Resource Service and returns a response containing resources and connection details by resource
     *
     * @param request the request
     * @return the resources by resource
     */
    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesByResource(@RequestBody final GetResourcesByResourceRequest request);

    /**
     * Post rest request to the Resource Service and returns a response containing connection details and resources by a specific resource type
     *
     * @param request the request
     * @return the resources by type
     */
    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesByType(@RequestBody final GetResourcesByTypeRequest request);

    /**
     * Post rest request to the Resource Service and returns a response containing resources and connection details by a specific date format
     * Resources of a particular data format may not share a type, e.g. not all CSV format records will contain employee contact details.
     *
     * @param request the request
     * @return the resources by serialised format
     */
    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/octet-stream")
    Response getResourcesBySerialisedFormat(@RequestBody final GetResourcesBySerialisedFormatRequest request);

    /**
     * Post request to the Resource Service which informs Palisade about a specific resource that it may return to users.
     * This lets Palisade clients request access to that resource and allows Palisade to provide policy controlled access
     * to it via the other methods in this interface.
     *
     * @param request the resource that Palisade can manage access to
     * @return whether or not the addResource call completed successfully
     */
    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    Boolean addResource(@RequestBody final AddResourceRequest request);
}
