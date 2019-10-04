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

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.request.*;

import java.util.Map;

@FeignClient(value = "resource-service")
public interface ResourceClient {

    @PostMapping(path = "/getResourcesById", consumes = "application/json", produces = "application/json")
    Map<LeafResource, ConnectionDetail> getResourcesById(final GetResourcesByIdRequest request);

    @PostMapping(path = "/getResourcesByResource", consumes = "application/json", produces = "application/json")
    Map<LeafResource, ConnectionDetail> getResourcesByResource(final GetResourcesByResourceRequest request);

    @PostMapping(path = "/getResourcesByType", consumes = "application/json", produces = "application/json")
    Map<LeafResource, ConnectionDetail> getResourcesByType(final GetResourcesByTypeRequest request);

    @PostMapping(path = "/getResourcesBySerialisedFormat", consumes = "application/json", produces = "application/json")
    Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final GetResourcesBySerialisedFormatRequest request);

    @PostMapping(path = "/addResource", consumes = "application/json", produces = "application/json")
    Boolean addResource(final AddResourceRequest request);

}
